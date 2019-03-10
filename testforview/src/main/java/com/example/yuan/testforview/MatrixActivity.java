package com.example.yuan.testforview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MatrixActivity extends AppCompatActivity {
    private static final String TAG = "MatrixActivity";
    private ImageView mImageView;
    private MyView mMyView;
    private Button mButton;
    private Button mButton4;
    private Button mButton5;
    private Button mButton6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matrix);

        init();
    }

    private void init() {
        mImageView = findViewById(R.id.imageView);
        mMyView = findViewById(R.id.myView);
        mButton = findViewById(R.id.button2);
        mButton4 = findViewById(R.id.button4);
        mButton5 = findViewById(R.id.button5);
        mButton6 = findViewById(R.id.button6);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.view);
        if (bitmap == null) {
            Log.e(TAG, "init: null --------");
        }
        mImageView.setImageBitmap(bitmap);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button2:
                        testTranslate();
                        break;
                    case R.id.button4:
                        testScale();
                        break;
                    case R.id.button5:
//                        testRotate();
                        testPolyToPoly();
                        break;
                    case R.id.button6:
                        testTranslate2();
                        break;
                }
            }
        };
        mButton.setOnClickListener(onClickListener);
        mButton4.setOnClickListener(onClickListener);
        mButton5.setOnClickListener(onClickListener);
        mButton6.setOnClickListener(onClickListener);
    }

    private void testRotate() {
        Matrix matrix = new Matrix();
        Log.d(TAG, "textMatrix: " + matrix.toString());
        matrix.postRotate(45);
        Log.d(TAG, "textMatrix: 旋转 改变  左上角" + matrix.toString());
        mMyView.setMatrix(matrix);
    }

    private void testScale() {
        Matrix matrix = new Matrix();
        Log.d(TAG, "textMatrix: " + matrix.toString());
        matrix.setScale(0.3f, 0.3f);
        Log.d(TAG, "textMatrix: 缩放 改变 左上对角线 " + matrix.toString());
        mMyView.setMatrix(matrix);
    }

    private void testTranslate() {
        Matrix matrix = new Matrix();
        Log.d(TAG, "textMatrix: " + matrix.toString());
        matrix.setTranslate(10, 100);
        matrix.preScale(0.3f, 0.3f);// 前乘
        Log.d(TAG, "textMatrix: 平移 改变 右上角 " + matrix.toString());
        mMyView.setMatrix(matrix);
    }

    private void testTranslate2() {
        Matrix matrix = new Matrix();
        Log.d(TAG, "textMatrix: " + matrix.toString());
        matrix.setTranslate(10, 100);
        matrix.postScale(0.3f, 0.3f);
        Log.e(TAG, "textMatrix: ***后乘就是matrix在后面，导致干扰平移*** " + matrix.toString());
        mMyView.setMatrix(matrix);
    }

    public void testPolyToPoly() {
        Matrix matrix = new Matrix();
        Log.d(TAG, "textMatrix: " + matrix.toString());
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.view);
        int bw = bitmap.getWidth() / 4;
        int bh = bitmap.getHeight() / 4;
        // 一个点
//        float[] src = {0, 0};// 左上顶点
//        int DX = 300;
//        float[] dst = {0 + DX, 0 + DX};
//        matrix.setPolyToPoly(src, 0, dst, 0, 1);
//        Log.e(TAG, "textMatrix: *** 一个点 平移 *** " + matrix.toString());
        // 两个点
//        Log.d(TAG, "testPolyToPoly: 图片 宽高：" + bw + "x" + bh);
//        float[] src = {bw / 2, bh / 2, bw, 0};// 中心点 右上顶点
//        float[] dst = {bw / 2, bh / 2, bw / 2 + bh / 2, bh / 2 + bw / 2};// 中心点 右下顶点
//        matrix.setPolyToPoly(src, 0, dst, 0, 2);
//        Log.e(TAG, "textMatrix: *** 两个点 旋转或者缩放 *** " + matrix.toString());
        // 三个点
//        float[] src = {0, 0, 0, bh, bw, bh};// 左上顶点 左下顶点 右下顶点
//        float[] dst = {0, 0, 200, bh, bw + 200, bh};
//        matrix.setPolyToPoly(src, 0, dst, 0, 3);
//        Log.e(TAG, "textMatrix: *** 三个点 错切 *** " + matrix.toString());
        // 4个点
        float[] src = {0, 0, 0, bh, bw, bh, bw, 0};// 左上顶点 左下顶点 右下顶点 右上顶点
        int DX = 100;
        float[] dst = {0 + DX, 0, 0, bh, bw, bh, bw - DX, 0};
        matrix.setPolyToPoly(src, 0, dst, 0, 4);
        Log.e(TAG, "textMatrix: *** 4个点，透视 只是把左右两个顶点往里面收拢了 *** " + matrix.toString());
        matrix.preScale(0.25f, 0.25f);
        mMyView.setMatrix(matrix);
    }
}
