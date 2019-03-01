package com.example.yuan.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;

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

    public int getJpegOrientation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int myDeviceOrientation = deviceOrientation;
        if (myDeviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0;
        }
        // Round device orientation to a multiple of 90
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
        int max = Math.max(width, height);
        int min = Math.min(width, height);
        Size size = new Size(max, min);
        float rate = 1.0f * max / min;
        Log.i(TAG, "getOptimalSize: width=" + width);
        Log.i(TAG, "getOptimalSize: height=" + height);
        Log.i(TAG, "getOptimalSize: rate=" + rate);
        // todo 选取规则
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
        for (int i = 0; i < sizeList.size(); i++) {
            Log.d(TAG, "getOptimalSize: 排序后" + sizeList.get(i).toString());
            if (sizeList.get(i).getWidth() == max && 1.0f * sizeList.get(i).getWidth() / sizeList.get(i).getHeight() - rate < 0.1) {
                size = sizeList.get(i);
                Log.i(TAG, "getOptimalSize: 已选取合适的size" + size);
//                break;
            }
        }
        return size;
    }

}
