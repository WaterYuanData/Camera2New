package com.example.yuan.camera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
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
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mStateCallback;
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback;
    private int mDeviceOrientation;
    private boolean mPause = false;
    private int mCaptureCount = 0;

    // 预览
    private TextureView mPreviewTextureView;
    private TextureView.SurfaceTextureListener mTextureListener;
    private Size mPreviewSize;
    private Surface mPreviewSurface;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCaptureSession mPreviewSession;
    private TextureView mPreviewTextureView2;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                Log.i(TAG, "onOrientationChanged: 设备方向 mDeviceOrientation=" + mDeviceOrientation);
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                // todo 在模拟器上不生效吗？一直是0
                Log.i(TAG, "onOrientationChanged: 窗口方向 getWindowManager=" + rotation);
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
        mPreviewTextureView.setSurfaceTextureListener(mTextureListener);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
        mPause = true;
        if (mCameraDevice != null) {
            mCameraDevice.close();
        } else {
            Log.e(TAG, "onPause: 相机未打开 故不需要关闭 请排查未打开的原因");
        }
        // mCameraHandler.sendMessage(mCameraHandler.obtainMessage(MSG_CLOSE_CAMERA));
    }

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
                        openCamera();
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
                    case R.id.button:
                        Log.d(TAG, "onClick: 拍照");
                        capture();
                        break;
                    case R.id.button2:
                        Log.d(TAG, "onClick: 再次预览");
                        startPreview();
                        break;
                }
            }
        };

        mPreviewTextureView = findViewById(R.id.textureView);
        findViewById(R.id.button).setOnClickListener(clickListener);
        findViewById(R.id.button2).setOnClickListener(clickListener);
        mThumbnail = findViewById(R.id.imageView);
        mPreviewTextureView2 = findViewById(R.id.textureView2);

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
                if (!mPause && mCameraDevice == null) {
                    Log.i(TAG, "onSurfaceTextureUpdated: 打开相机 ... ");
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
                Log.e(TAG, "onError: " + error);
                camera.close();
            }

            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                Log.i(TAG, "onClosed: ");
                mCameraDevice = null;
            }
        };

        mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            // todo
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                // Log.d(TAG, "onCaptureCompleted: 完成 会一直打印");
                if (mCaptureCount % 200 == 0) {
                    Integer integer = request.get(CaptureRequest.JPEG_ORIENTATION);
                    Log.e(TAG, "onCaptureCompleted: yyyy " + integer);
                    Log.e(TAG, "onCaptureCompleted: yyyy " + request.toString() + " hashCode " + request.hashCode());
                    Integer it = result.get(CaptureResult.JPEG_ORIENTATION);
                    Log.e(TAG, "onCaptureCompleted: yyyy ** " + it);
                }
                mCaptureCount++;
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                // Log.d(TAG, "onCaptureProgressed: 过程中 会一直打印");
            }
        };
    }

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


    // 创建保存图片的线程
    public static class ImageSaverTask implements Runnable {
        private Image mImage;
        private Handler mCameraHandler;

        ImageSaverTask(Image image, Handler handler) {
            mImage = image;
            mCameraHandler = handler;
        }

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
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //设置CaptureRequest输出到mImageReader
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            //将本次capture也输出到预览的surface上，避免卡顿 todo 然而效果不明显
            mCaptureBuilder.addTarget(mPreviewSurface);
            //设置拍照方向
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            Log.d(TAG, "capture: 窗口方向=" + rotation + " 修正后的拍照方向=" + ORIENTATION.get(rotation));
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
//            mPreviewSession.stopRepeating();****************拍照不需要关闭预览
            //开始拍照，然后回调上面的接口重启预览，因为mCaptureBuilder设置ImageReader作为target，所以会自动回调ImageReader的onImageAvailable()方法保存图片
            mPreviewSession.capture(mCaptureBuilder.build(), mImageSavedCallback, mCameraHandler);
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
        setupImageReader();

        //获取ImageReader的Surface
        Surface imageReaderSurface = mImageReader.getSurface();

        //CaptureRequest添加imageReaderSurface，不加的话就会导致ImageReader的onImageAvailable()方法不会回调
//        mCaptureRequestBuilder.addTarget(imageReaderSurface);

        //创建CaptureSession时加上imageReaderSurface，如下，这样预览数据就会同时输出到previewSurface和imageReaderSurface了
        // mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReaderSurface), mSessionCaptureCallback);
    }


    private void setupCamera(int width, int height) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍历所有摄像头
            assert manager != null;
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                Log.d(TAG, "setupCamera: 打开相机前 查看相机的参数");

                //默认打开后置摄像头
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                Integer hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                Log.d(TAG, "setupCamera: 相机支持的等级（0是最低级）=" + hardwareLevel);

                Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.d(TAG, "setupCamera: 相机的传感器方向=" + sensorOrientation);

                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //根据TextureView的尺寸设置预览尺寸
                assert map != null;
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                Log.d(TAG, "setupCamera: mPreviewSize=" + mPreviewSize.toString());
                mCameraId = cameraId;

                boolean outputSupportedFor = map.isOutputSupportedFor(ImageFormat.YUV_420_888);
                Log.i(TAG, "setupCamera: 是否支持YUV_420_888 " + outputSupportedFor);

                Size[] sizes = characteristics.get(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES);
                for (int i = 0; i < sizes.length; i++) {
                    Log.d(TAG, "setupCamera: 缩略图尺寸 " + sizes[i]);
                }

                // Camera2拍照也是通过ImageReader来实现的
                mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                    }
                });
                Log.d(TAG, "setupCamera: mCaptureSize=" + mCaptureSize.toString());

                break;
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
     * getOptimalSize: 1200x1200
     * getOptimalSize: 1280x480
     * getOptimalSize: 1280x400
     * getOptimalSize: 1024x768
     * getOptimalSize: 800x600
     * getOptimalSize: 864x480
     * getOptimalSize: 800x480
     * getOptimalSize: 720x480
     * getOptimalSize: 640x480
     * getOptimalSize: 640x360
     * 02-01 22:53:44.046 18670-18670/com.example.yuan.camera2 I/Camera2: getOptimalSize: 480x640
     * getOptimalSize: 480x360
     * getOptimalSize: 480x320
     * getOptimalSize: 352x288
     * getOptimalSize: 320x240
     * getOptimalSize: 240x320
     * getOptimalSize: 176x144
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

    private void startPreview() {

        setupImageReader();

        SurfaceTexture mSurfaceTexture = mPreviewTextureView.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //获取Surface显示预览数据
        mPreviewSurface = new Surface(mSurfaceTexture);

        SurfaceTexture surfaceTexture = mPreviewTextureView2.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);

        try {
            //创建请求的Builder，TEMPLATE_PREVIEW表示预览请求
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //设置Surface作为预览数据的显示界面
            mPreviewRequestBuilder.addTarget(mPreviewSurface);

            mPreviewRequestBuilder.addTarget(surface);

            // 调整方向
            // mCaptureRequestBuilder.set();

            //创建预览会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface, mImageReader.getSurface(),surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.i(TAG, "onConfigured: 创建createCaptureSession的状态回调 线程名 " + Thread.currentThread().getName());
                    try {
                        //预览请求成功后得到预览会话
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mPreviewSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        Log.d(TAG, "onConfigured: " + mPreviewRequest.toString() + " hashCode " + mPreviewRequest.hashCode());
                        mPreviewSession.setRepeatingRequest(mPreviewRequest, mSessionCaptureCallback, mCameraHandler);
                        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(MainActivity.MSG_PREVIEW_STARTED));
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupImageReader() {
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据，本例的2代表ImageReader中最多可以获取两帧图像流
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                ImageFormat.JPEG, 2);
        //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                //我们可以将这帧数据转成字节数组，类似于Camera1的PreviewCallback回调的预览帧数据
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                image.close();
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
