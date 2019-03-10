package com.example.yuan.camera2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE = 500;

    private String mCameraId;
    private int[] mCameraIdInt;
    private CameraCharacteristics[] mCameraCharacteristicArrays;
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mStateCallback;
    private CameraCaptureSession mCaptureSession;
    private int mDeviceOrientation;
    private boolean mPause;
    private boolean mNeedOpen;

    private int mDisplayOrientation;
    private int mNeedRotation;
    private Integer mSensorOrientation;

    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    /*
      屏幕方向=0 拍照方向=90
      屏幕方向=1 拍照方向=0
      屏幕方向=3 拍照方向=180
      */
    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    // 预览
    private TextureView mPreviewTextureView;
    private Surface mPreviewSurface;
    private TextureView.SurfaceTextureListener mTextureListener;
    private Size mPreviewSize;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback;
    private int mRepeatCaptureCount;
    // 多预览
    private TextureView mPreviewTextureView2;
    private Surface mPreviewSurface2;
    // 用ImageReader做预览，练习调整方向
    private ImageReader mPreviewImageReader;

    // 拍照
    private ImageReader mImageReader;
    private Size mCaptureSize;
    private static File mImageFile;

    private Handler mCameraHandler;
    private static final int MSG_OPEN_CAMERA = 1;
    private static final int MSG_CLOSE_CAMERA = 0;
    private static final int MSG_PICTURE_SAVED = 10;
    private static final int MSG_PREVIEW_STARTED = 2;
    private ImageView mThumbnail;
    private MyLayout mMyLayout;
    private Rect mRect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 隐藏状态栏
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        init();
    }

    public class MyOrientationEventListener extends OrientationEventListener {
        MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                return;
            }
            //保证只返回四个方向
            int newOrientation = ((orientation + 45) / 90 * 90) % 360;
            if (newOrientation != mDeviceOrientation) {
                //返回的mDeviceOrientation就是手机方向，为0°、90°、180°和270°中的一个
                mDeviceOrientation = newOrientation;
                switch (mDeviceOrientation) {
                    case 0:
                        mDisplayOrientation = mDeviceOrientation;
                        break;
                    case 90:
                        mDisplayOrientation = 270;
                        break;
                    case 180:
                        mDisplayOrientation = 180;
                        break;
                    case 270:
                        mDisplayOrientation = 90;
                        break;
                }
                switch (mDisplayOrientation) {
                    case 0:
                        mNeedRotation = mSensorOrientation;
                        break;
                    case 90:
                        mNeedRotation = 180;
                        break;
                    case 180:
                        mNeedRotation = 270;
                        break;
                    case 270:
                        mNeedRotation = 0;
                        break;
                }
                Log.i(TAG, "onOrientationChanged: 设备方向 mDeviceOrientation=" + mDeviceOrientation);
                Log.i(TAG, "onOrientationChanged: 显示方向 mDisplayOrientation=" + mDisplayOrientation);
                Log.i(TAG, "onOrientationChanged: 需要旋转 mNeedRotation=" + mNeedRotation);
            }
            /*
              0<=orientation<45 则 newOrientation=0
              45<=orientation<135 则 newOrientation=90
              135<=orientation<225 则 newOrientation=180
              225<=orientation<315 则 newOrientation=270
              315<=orientation<315 则 newOrientation=0
              */
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: 线程名 " + Thread.currentThread().getName());
        super.onResume();
        mPause = false;
        if (mPreviewTextureView2.isAvailable()) {
            Log.d(TAG, "onResume: mPreviewTextureView2 可用");
            mCameraHandler.sendMessage(mCameraHandler.obtainMessage(MSG_OPEN_CAMERA));
        } else {
            Log.d(TAG, "onResume: mPreviewTextureView2 不可用");
            mPreviewTextureView2.setSurfaceTextureListener(mTextureListener);
        }
        mPreviewTextureView.setAlpha(0);
        hideSystemBars();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
        mPause = true;
        mNeedOpen = true;
        if (mCameraDevice != null) {
            mCameraDevice.close();
        } else {
            Log.e(TAG, "onPause: 相机未打开 故不需要关闭 请排查未打开的原因");
        }
        // mCameraHandler.sendMessage(mCameraHandler.obtainMessage(MSG_CLOSE_CAMERA));
    }

    private void hideSystemBars() {
        // Please note: this method must be called both in onResume() and onWindowFocusChanged() !!!
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void init() {

        MyOrientationEventListener myOrientationEventListener = new MyOrientationEventListener(getApplicationContext());
        if (myOrientationEventListener.canDetectOrientation()) {
            myOrientationEventListener.enable();
        }

        HandlerThread cameraThread = new HandlerThread("Camera2");
        cameraThread.start();
        mCameraHandler = new Handler(cameraThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_OPEN_CAMERA:
                        if (mNeedOpen) {
                            mNeedOpen = false;
                            openCamera();
                        } else {
                            Log.d(TAG, "handleMessage: 不需要再openCamera");
                        }
                        break;
                    case MSG_CLOSE_CAMERA:
                        if (mCameraDevice != null) {
                            mCameraDevice.close();
                        }
                        break;
                    case MSG_PICTURE_SAVED:
                        Log.i(TAG, "handleMessage: 照片已保存");
                        // todo 是否需要关闭 拍照的mImageReader 如果需要则先调整其初始化的位置
                        final Bitmap thumbnail = getThumbnail(mImageFile.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mThumbnail.setImageBitmap(thumbnail);
                            }
                        });
                        break;
                    case MSG_PREVIEW_STARTED:
                        Log.i(TAG, "handleMessage: 预览已开启");
                        break;
                }

            }
        };

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.takePicture:
                        Log.d(TAG, "onClick: 拍照");
                        capture();
                        break;
                    case R.id.button2:
                        Log.d(TAG, "onClick: 再次预览");
//                        startPreview();
                        break;
                }
            }
        };

        mPreviewTextureView = findViewById(R.id.textureView);
        findViewById(R.id.takePicture).setOnClickListener(clickListener);
        findViewById(R.id.button2).setOnClickListener(clickListener);
        mThumbnail = findViewById(R.id.thumbnail);
        mPreviewTextureView2 = findViewById(R.id.textureView2);

        // region 手势层：处理点击对焦
        mMyLayout = findViewById(R.id.myLayout);
        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 相对于手机自然状态即竖屏的左上顶点的坐标
                float eventX = event.getX();
                float eventY = event.getY();
                Log.i(TAG, "onTouch: eventX=" + eventX + " eventY=" + eventY);
                mMyLayout.setEventX(eventX);
                mMyLayout.setEventY(eventY);
                mMyLayout.invalidate();
                //
                int width = mMyLayout.getWidth();
                int height = mMyLayout.getHeight();
                int radius = mMyLayout.getRadius();
                Log.i(TAG, "onTouch: width=" + width + " height=" + height + " radius=" + radius);
                int left = (int) (eventX - radius);
                int top = (int) (eventY - radius);
                int right = (int) (eventX + radius);
                int bottom = (int) (eventY + radius);
                if (left < 0) left = 0;
                if (top < 0) top = 0;
                if (right > width) right = width;
                if (bottom > height) bottom = height;
                Rect rect = new Rect(left, top, right, bottom);
                Log.d(TAG, "onTouch: 变换前矩形 " + rect.toString());
                // todo 屏幕坐标向相机坐标转换
                float newX = eventY;
                float newY = width - eventX;
                int x = (int) (newX / height * mRect.width());
                int y = (int) (newY / width * mRect.height());
                Log.d(TAG, "onTouch: width=" + mRect.width() + " left=" + mRect.right);
                left = clamp(x - radius, 0, mRect.right);
                top = clamp(y - radius, 0, mRect.bottom);
                right = clamp(x + radius, 0, mRect.right);
                bottom = clamp(y + radius, 0, mRect.bottom);
                rect = new Rect(left, top, right, bottom);
                Log.i(TAG, "onTouch: 变换后矩形 " + rect.toString());
                getPreviewRequestBuilder();
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect, 1000)});
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect, 1000)});
//                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                repeatPreview();
                //触发对焦
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
                try {
                    //触发对焦通过capture发送请求, 因为用户点击屏幕后只需触发一次对焦
                    mCaptureSession.capture(mPreviewRequestBuilder.build(), mPreviewCaptureCallback, mCameraHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                return false;
            }
        };
        mMyLayout.setOnTouchListener(onTouchListener);
        // endregion

        mTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureAvailable: 当SurefaceTexture可用的时候，设置相机参数并打开相机 width=" + width + " height=" + height);
                //当SurefaceTexture可用的时候，设置相机参数并打开相机
                setupCamera(width, height);
                mCameraHandler.sendMessage(mCameraHandler.obtainMessage(MSG_OPEN_CAMERA));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureSizeChanged: ");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG, "onSurfaceTextureDestroyed: ");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // Log.d(TAG, "onSurfaceTextureUpdated: 会一直打印");
                /*
                 * 1. 预览启动后该方法一直被调用的原因是setRepeatingRequest导致onCaptureCompleted一直被调用，
                 *    及与其target连接的surface一直被调用
                 * 2. Home Recent Power 从 onPause 到 onResume 对 TextureView 生命周期的影响：
                 *    ① Home 的 onPause：onClosed onSurfaceTextureUpdated onSurfaceTextureDestroyed
                 *       Home 的 onResume：onSurfaceTextureAvailable  openCamera
                 *    ② Recent 的 onPause：onClosed onSurfaceTextureDestroyed
                 *       Recent 的 onResume：onSurfaceTextureAvailable onSurfaceTextureUpdated openCamera
                 *    ③ Power 的 onPause：onClosed onSurfaceTextureUpdated
                 *       Power 的 onResume：isAvailable openCamera
                 * */
                if (mPause) {
                    Log.d(TAG, "onSurfaceTextureUpdated: onPause() 执行一次");
                } else if (mCameraDevice == null) {
                    Log.i(TAG, "onSurfaceTextureUpdated: onPause()后的onResume() 且 mCameraDevice==null 下，执行一次");
                    mCameraHandler.sendMessage(mCameraHandler.obtainMessage(MSG_OPEN_CAMERA));
                }
            }
        };

        mStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d(TAG, "onOpened: 已打开相机,接下来开始预览 线程名 " + Thread.currentThread().getName());
                mCameraDevice = camera;
                //开启预览
                startPreview();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.d(TAG, "onDisconnected: ");
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                if (error == CameraDevice.StateCallback.ERROR_CAMERA_IN_USE) {
                    Log.e(TAG, "onError: 相机已被占用" + error);
                } else {
                    Log.e(TAG, "onError: 打开相机出错 " + error);
                }
                camera.close();
            }

            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                Log.i(TAG, "onClosed: 相机已关闭");
                mCameraDevice = null;
            }
        };

        mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                // Log.d(TAG, "onCaptureCompleted: 完成 会一直打印"); 导致onSurfaceTextureUpdated被一直打印
                if (request == mPreviewRequest) {
                    if (mRepeatCaptureCount % 200 == 0) { // 200次大约10秒
                        Integer requestOrientation = request.get(CaptureRequest.JPEG_ORIENTATION);
                        Integer resultOrientation = result.get(CaptureResult.JPEG_ORIENTATION);
                        Log.d(TAG, "onCaptureCompleted: 验证hashCode值 " + request);
                        Log.d(TAG, "onCaptureCompleted: mRepeatCaptureCount=" + mRepeatCaptureCount);
//                        Log.d(TAG, "onCaptureCompleted: requestOrientation=" + requestOrientation + " resultOrientation=" + resultOrientation);

                        MeteringRectangle[] meteringRectangles = request.get(CaptureRequest.CONTROL_AF_REGIONS);
                        if (meteringRectangles != null && meteringRectangles.length > 0) {
                            Log.d(TAG, "onCaptureCompleted: getRect=" + meteringRectangles[0].getRect().toString());
                        }

                        // region 已验证可行
                        if (request == mPreviewRequest) {
                            if (mRepeatCaptureCount == 0) {
                                mCameraHandler.sendMessage(mCameraHandler.obtainMessage(MainActivity.MSG_PREVIEW_STARTED));
                            }
                            Log.d(TAG, "onCaptureCompleted: 预览已启动 ");
                        }
                        // endregion
                    }
                    mRepeatCaptureCount++;
                } else {
                    Log.d(TAG, "onCaptureCompleted: 非预览");
                }
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                // Log.d(TAG, "onCaptureProgressed: 过程中 会一直打印");
            }
        };
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    // 创建保存图片的线程
    public static class ImageSaverTask implements Runnable {
        private Image mImage;
        private Handler mCameraHandler;

        ImageSaverTask(Image image, Handler handler) {
            mImage = image;
            mCameraHandler = handler;
        }

        @SuppressWarnings("all")
        @Override
        public void run() {
            Log.i(TAG, "run: 执行保存照片子线程 线程名 " + Thread.currentThread().getName());
            Log.i(TAG, "run: 执行保存照片子线程 " + mImage.toString());
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            new File(Environment.getExternalStorageDirectory() + "/DCIM/").mkdirs();
            mImageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/Pic_" + System.currentTimeMillis() + ".jpg");
            try {
                Log.d(TAG, "run: mImageFile=" + mImageFile.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mImageFile);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (mImage != null) {
                    mImage.close();
                    mImage = null;
                }
                mCameraHandler.sendMessage(mCameraHandler.obtainMessage(MainActivity.MSG_PICTURE_SAVED));
            }
        }
    }

    private void capture() {
        try {
            //首先我们创建请求拍照的CaptureRequest
            CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //设置CaptureRequest输出到mImageReader
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            //将本次capture也输出到预览的surface上，避免卡顿 todo 然而效果不明显
            mCaptureBuilder.addTarget(mPreviewSurface2);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //设置拍照方向,为何用状态栏方向去修正拍照方向？？？
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            Log.d(TAG, "capture: 状态栏方向=" + rotation + " 修正后的拍照方向=" + ORIENTATION.get(rotation));
            //这个回调接口用于拍照结束时重启预览，因为拍照会导致预览停止
            CameraCaptureSession.CaptureCallback mImageSavedCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Toast.makeText(getApplicationContext(), "拍照已完成", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onCaptureCompleted: 拍照已完成");
                    //重启预览
//                    startPreview();
                }
            };
            //停止预览
//            mCaptureSession.stopRepeating();****************拍照不需要关闭预览
            //开始拍照，然后回调上面的接口重启预览，因为mCaptureBuilder设置ImageReader作为target，所以会自动回调ImageReader的onImageAvailable()方法保存图片
            mCaptureSession.capture(mCaptureBuilder.build(), mImageSavedCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * https://www.cnblogs.com/renhui/p/8718758.html
     * 5、实现PreviewCallback
     */
    @SuppressWarnings("unused")
    public void beforeStartPreview() {

    }

    @SuppressWarnings("all")
    private void setupCamera(int width, int height) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍历所有摄像头
            assert manager != null;
            mCameraCharacteristicArrays = new CameraCharacteristics[manager.getCameraIdList().length];
            mCameraIdInt = new int[manager.getCameraIdList().length];
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                Log.d(TAG, "setupCamera: 打开相机前 查看相机的参数 " + cameraId);
                mCameraIdInt[Integer.parseInt(cameraId)] = Integer.parseInt(cameraId);
                mCameraCharacteristicArrays[Integer.parseInt(cameraId)] = characteristics;

                //默认打开后置摄像头
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                mCameraId = cameraId;

                Integer hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                Log.d(TAG, "setupCamera: 相机支持的等级（0是最低级）=" + hardwareLevel);

                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.d(TAG, "setupCamera: 相机传感器方向=" + mSensorOrientation);
                mNeedRotation = mSensorOrientation;

                mRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                Log.d(TAG, "setupCamera: 相机坐标 " + mRect.toString());

                // 均为null，说明不支持？？？
                Float min = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                Integer cal = characteristics.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION);
                Log.d(TAG, "setupCamera: 焦距 " + min + " " + cal);

                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //根据TextureView的尺寸设置预览尺寸
                assert map != null;
                width = mPreviewTextureView2.getWidth();
                height = mPreviewTextureView2.getHeight();
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                Log.d(TAG, "setupCamera: mPreviewSize=" + mPreviewSize.toString());

                boolean outputSupportedFor = map.isOutputSupportedFor(ImageFormat.YUV_420_888);
                Log.i(TAG, "setupCamera: 是否支持YUV_420_888 " + outputSupportedFor);

                Size[] sizes = characteristics.get(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES);
                assert sizes != null;
                for (Size size : sizes) {
                    Log.d(TAG, "setupCamera: 缩略图尺寸 " + size);
                }

                // Camera2拍照也是通过ImageReader来实现的
                mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                    }
                });
                Log.d(TAG, "setupCamera: mCaptureSize=" + mCaptureSize.toString());

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getThumbnail(String jpegPath) {
        Bitmap thumbnailBitmap = null;
        try {
            ExifInterface exifInterface = new ExifInterface(jpegPath);
            int orientationInt = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            float orientationFloat = 0.0f;
            switch (orientationInt) {
                case ExifInterface.ORIENTATION_NORMAL:
                    orientationFloat = 0.0f;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientationFloat = 90.0f;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    orientationFloat = 180.0f;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientationFloat = 270.0f;
                    break;
            }
            // 假设图片在 Exif 写入缩略图
            if (exifInterface.hasThumbnail()) {
                Log.d(TAG, "getThumbnail: ");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    thumbnailBitmap = exifInterface.getThumbnailBitmap();
                }
            }
//            byte[] thumbnail = exifInterface.getThumbnail();区别
            // 假设未写入
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 16;
            thumbnailBitmap = BitmapFactory.decodeFile(jpegPath, options);
            if (orientationFloat != 0.0f && thumbnailBitmap != null) {
                Matrix matrix = new Matrix();
                matrix.setRotate(orientationFloat);
                thumbnailBitmap = Bitmap.createBitmap(thumbnailBitmap, 0, 0, thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight(), matrix, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return thumbnailBitmap;
    }

    //region getOptimalSize

    /**
     * 02-01 22:53:44.044 18670-18670/com.example.yuan.camera2 I/Camera2: getOptimalSize: 4056x3040
     * getOptimalSize: 4056x2704
     * getOptimalSize: 4000x3000
     * getOptimalSize: 3840x2160
     * getOptimalSize: 3264x2448
     * getOptimalSize: 3200x2400
     * getOptimalSize: 2976x2976
     * getOptimalSize: 2592x1944
     * getOptimalSize: 2560x1440
     * 02-01 22:53:44.045 18670-18670/com.example.yuan.camera2 I/Camera2: getOptimalSize: 2688x1512
     * getOptimalSize: 2048x1536
     * getOptimalSize: 1920x1080
     * getOptimalSize: 2560x800
     * getOptimalSize: 1600x1200
     * getOptimalSize: 1440x1080
     * getOptimalSize: 1280x960
     * getOptimalSize: 1280x768
     * getOptimalSize: 1280x720
     */
    //endregion
    public Size getOptimalSize(Size[] sizes, int width, int height) {
        return MyCameraUtils.getInstance().getOptimalSize(sizes, width, height);
    }


    private void openCamera() {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //检查权限
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
                return;
            }
            Log.i(TAG, "openCamera: 线程名 " + Thread.currentThread().getName());
            //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            assert manager != null;
            manager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void getPreviewRequestBuilder() {
        //创建请求的Builder，TEMPLATE_PREVIEW表示预览请求
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //设置预览的显示界面
//        mPreviewRequestBuilder.addTarget(mPreviewSurface);
        // 多预览
        mPreviewRequestBuilder.addTarget(mPreviewSurface2);
//        mPreviewRequestBuilder.addTarget(mPreviewImageReader.getSurface());
        Log.d(TAG, "getPreviewRequestBuilder: hashCode=" + mPreviewRequestBuilder);
    }

    private void repeatPreview() {
        mPreviewRequest = mPreviewRequestBuilder.build();
        try {
            //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mPreviewCaptureCallback, mCameraHandler);
            mRepeatCaptureCount = 0;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "repeatPreview: hashCode=" + mPreviewRequest);
    }

    private void startPreview() {

        setupImageReader();

        SurfaceTexture mSurfaceTexture = mPreviewTextureView.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //获取Surface显示预览数据
        mPreviewSurface = new Surface(mSurfaceTexture);

        // 多预览
        SurfaceTexture surfaceTexture = mPreviewTextureView2.getSurfaceTexture();
        Log.i(TAG, "startPreview: " + mPreviewTextureView2.getWidth() + "x" + mPreviewTextureView2.getHeight() + " : " + (1.f * mPreviewTextureView2.getHeight() / mPreviewTextureView2.getWidth()));
        Log.i(TAG, "startPreview: " + mPreviewSize.toString() + " : " + (1.f * mPreviewSize.getWidth() / mPreviewSize.getHeight()));
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mPreviewSurface2 = new Surface(surfaceTexture);

        try {
            getPreviewRequestBuilder();

            // 矫正TextureView预览 已验证只能强制竖屏，否则非强制竖屏如下方法不行，不强制竖屏如何矫正？？？
            Log.i(TAG, "startPreview: 屏幕显示方向=" + mDisplayOrientation + " 需要旋转=" + mNeedRotation);
//            mPreviewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mNeedRotation);

            // 已验证可以，矫正ImageReader的预览，为何用状态栏方向去修正ImageReader的预览方向？？？
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            Log.e(TAG, "startPreview: 状态栏方向=" + rotation + " 修正预览=" + ORIENTATION.get(rotation));
//            mPreviewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));

            //创建预览会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            mCameraDevice.createCaptureSession(
                    Arrays.asList(mPreviewSurface, mPreviewSurface2,
                            mImageReader.getSurface(), mPreviewImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            Log.i(TAG, "onConfigured: 创建createCaptureSession的状态回调 线程名 " + Thread.currentThread().getName());
                            //预览请求成功后得到预览会话
                            mCaptureSession = session;
                            repeatPreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "onConfigureFailed: 创建CameraCaptureSession失败");
                        }
                    }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupImageReader() {
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据，本例的2代表ImageReader中最多可以获取两帧图像流
        mPreviewImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                ImageFormat.JPEG, 20);
        //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
        mPreviewImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                final Image image = reader.acquireLatestImage();
                //我们可以将这帧数据转成字节数组，类似于Camera1的PreviewCallback回调的预览帧数据
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mThumbnail.setImageBitmap(bitmap);
                        image.close();
                    }
                });
            }
        }, null);

        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //执行图像保存子线程
                Log.d(TAG, "onImageAvailable: 调用保存照片子线程");
                Image nextImage = reader.acquireNextImage();
                Log.i(TAG, "onImageAvailable: " + nextImage.toString());
                mCameraHandler.post(new ImageSaverTask(nextImage, mCameraHandler));
//                reader.close(); reader必须在子线程结束后才能关闭，因为reader的关闭会使关闭nextImage，而通过引用传递到子线程中的mImage也会立即关闭
            }
        }, mCameraHandler);
    }

}
