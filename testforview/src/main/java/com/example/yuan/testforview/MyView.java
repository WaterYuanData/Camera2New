package com.example.yuan.testforview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class MyView extends FrameLayout {

    private final Paint mPaint;
    private final Bitmap mBitmap;
    private Matrix mMatrix;

    public MyView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.view);
        mMatrix = new Matrix();
    }

    public void setMatrix(Matrix matrix) {
        mMatrix = matrix;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, mMatrix, mPaint);
    }
}
