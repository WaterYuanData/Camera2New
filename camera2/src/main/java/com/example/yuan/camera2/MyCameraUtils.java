package com.example.yuan.camera2;

import android.app.Activity;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyCameraUtils {
    private static final String TAG = "MyCameraUtils";
    private static MyCameraUtils mCameraUtils;

    private MyCameraUtils() {
    }

    public static MyCameraUtils getInstance() {
        if (mCameraUtils == null) {
            mCameraUtils = new MyCameraUtils();
        }
        return mCameraUtils;
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    public int getJpegOrientation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int myDeviceOrientation = deviceOrientation;
        if (myDeviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0;
        }
        // Round device orientation to a multiple of 90
        /*
         * <45 >315 0 0
         * 45<= <135 1 90
         * 135<= <225 2 180
         * 225<= <315 3 270
         * */
        myDeviceOrientation = (myDeviceOrientation + 45) / 90 * 90;
        // Reverse device orientation for front-facing cameras
        Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            myDeviceOrientation = -myDeviceOrientation;
        }
        Integer sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + myDeviceOrientation + 360) % 360;
    }

    public Size getOptimalSize(Size[] sizes, int width, int height) {
        Size size = null;
        int max = Math.max(width, height);
        int min = Math.min(width, height);
        float rate = 1.0f * max / min;
        Log.i(TAG, "getOptimalSize: 输入= " + width + "x" + height + " rate=" + rate);
        // todo 选取规则
        long currentTimeMillis = System.currentTimeMillis();
        List<Size> sizeList = Arrays.asList(sizes);
        Collections.sort(sizeList, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.getWidth() > o2.getWidth()) {
                    // 返回负数才会改变顺序
                    return -1;
                }
                if (o1.getHeight() > o2.getHeight()) {
                    return -1;
                }
                return 0;
            }
        });
        Log.d(TAG, "getOptimalSize: 排序耗时 " + (System.currentTimeMillis() - currentTimeMillis));
        boolean find = false;
        for (int i = 0; i < sizeList.size(); i++) {
            Log.d(TAG, "getOptimalSize: 排序后" + sizeList.get(i).toString());
            if (!find) {
                if (Math.abs(1.0f * sizeList.get(i).getWidth() / sizeList.get(i).getHeight() - rate) < 0.1f) {
                    if (sizeList.get(i).getWidth() < max) {
                        size = sizeList.get(i);
                        find = true;
                        Log.i(TAG, "getOptimalSize: 已找到合适的预览尺寸" + size);
                    }
                }
            }
        }
        if (!find) {
            Log.e(TAG, "getOptimalSize: 未找到合适的预览尺寸");
        }
        return size;
    }

}
