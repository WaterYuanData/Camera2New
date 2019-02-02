package com.example.yuan.camera2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
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
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Camera2";
    private static final int REQUEST_CODE = 500;
    private static File mImageFile;

    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mTextureListener;
    private String mCameraId;
    private CameraDevice.StateCallback mStateCallback;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mPreviewSession;
    private ImageReader mImageReader;
    private Size mPreviewSize;
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback;
    private Integer mSensorOrientation;
    private View.OnClickListener mClickListener;
    private Size mCaptureSize;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }


    @Override
    protected void onResume() {
        super.onResume();
        mTextureView.setSurfaceTextureListener(mTextureListener);
    }

    public void init() {

        mHandlerThread = new HandlerThread("Camera2");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button:
                        Log.d(TAG, "onClick: 拍照");
                        capture();
                        break;
                    case R.id.button2:
                        break;
                }
            }
        };

        mTextureView = findViewById(R.id.textureView);
        findViewById(R.id.button).setOnClickListener(mClickListener);
        findViewById(R.id.button2).setOnClickListener(mClickListener);

        mTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureAvailable: 当SurefaceTexture可用的时候，设置相机参数并打开相机 width=" + width + " height=" + height);
                //当SurefaceTexture可用的时候，设置相机参数并打开相机
                setupCamera(width, height);
                openCamera();
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
            }
        };

        mStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                Log.d(TAG, "onOpened: 已打开相机,接下来开始预览");
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
                Log.d(TAG, "onError: ");
            }
        };

        mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            // todo
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                // Log.d(TAG, "onCaptureCompleted: 完成 会一直打印");
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                // Log.d(TAG, "onCaptureProgressed: 过程中 会一直打印");
            }
        };
    }

    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }


    // 创建保存图片的线程
    public static class imageSaver implements Runnable {
        private Image mImage;

        public imageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            mImageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/myPicture.jpg");
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
                mImageFile = null;
                if (fos != null) {
                    try {
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void capture() {
        try {
            //首先我们创建请求拍照的CaptureRequest
            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //设置CaptureRequest输出到mImageReader
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            //设置拍照方向
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            //这个回调接口用于拍照结束时重启预览，因为拍照会导致预览停止
            CameraCaptureSession.CaptureCallback mImageSavedCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    Toast.makeText(getApplicationContext(), "Image Saved!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onCaptureCompleted: Image Saved!");
                    //重启预览
                    // restartPreview();
                }
            };
            //停止预览
            mPreviewSession.stopRepeating();
            //开始拍照，然后回调上面的接口重启预览，因为mCaptureBuilder设置ImageReader作为target，所以会自动回调ImageReader的onImageAvailable()方法保存图片
            mPreviewSession.capture(mCaptureBuilder.build(), mImageSavedCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * https://www.cnblogs.com/renhui/p/8718758.html
     * 5、实现PreviewCallback
     */
    public void beforeStartPreview() {
        setupImageReader();

        //获取ImageReader的Surface
        Surface imageReaderSurface = mImageReader.getSurface();

        //CaptureRequest添加imageReaderSurface，不加的话就会导致ImageReader的onImageAvailable()方法不会回调
        mCaptureRequestBuilder.addTarget(imageReaderSurface);

        //创建CaptureSession时加上imageReaderSurface，如下，这样预览数据就会同时输出到previewSurface和imageReaderSurface了
        // mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReaderSurface), mSessionCaptureCallback);
    }


    private void setupCamera(int width, int height) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍历所有摄像头
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                //默认打开后置摄像头
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.d(TAG, "setupCamera: 方向=" + mSensorOrientation);

                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //根据TextureView的尺寸设置预览尺寸
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                Log.d(TAG, "setupCamera: mPreviewSize=" + mPreviewSize.toString());
                mCameraId = cameraId;

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
        Size size;
        for (int i = 0; i < sizes.length; i++) {
            // todo
            // Log.i(TAG, "getOptimalSize: " + sizes[i].toString());
        }
        size = sizes[8];
        size = sizes[25];
        return size;
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
            //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            manager.openCamera(mCameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {

        setupImageReader();

        SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //获取Surface显示预览数据
        Surface mSurface = new Surface(mSurfaceTexture);
        try {
            //创建CaptureRequestBuilder，TEMPLATE_PREVIEW比表示预览请求
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //设置Surface作为预览数据的显示界面
            mCaptureRequestBuilder.addTarget(mSurface);

            // 调整方向
            // mCaptureRequestBuilder.set();

            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.i(TAG, "onConfigured: 创建createCaptureSession的状态回调");
                    try {
                        //创建捕获请求
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mPreviewSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, mSessionCaptureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
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
                Log.d(TAG, "onImageAvailable: 执行图像保存子线程");
                mHandler.post(new imageSaver(reader.acquireNextImage()));
            }
        }, mHandler);
    }

}
