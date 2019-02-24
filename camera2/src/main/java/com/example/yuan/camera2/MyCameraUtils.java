package com.example.yuan.camera2;

import android.util.Log;
import android.util.Size;

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

    public Size getOptimalSize(Size[] sizes, int width, int height) {
        Size size = new Size(width, height);
        int max = Math.max(width, height);
        int min = Math.min(width, height);
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
