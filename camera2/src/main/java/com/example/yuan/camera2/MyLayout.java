package com.example.yuan.camera2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class MyLayout extends FrameLayout {
    private static final String TAG = "MyLayout";
    private float mEventX;
    private float mEventY;
    private int mRadius = 200;
    private int mStrokeWidth = 4;
    private Paint mPaint;

    public MyLayout(@NonNull Context context) {
        super(context);
    }

    public MyLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(R.color.colorAccent));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);
        OnTouchListener onTouchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mEventX = event.getX();
                mEventY = event.getY();
                Log.i(TAG, "onTouch: eventX=" + mEventX + " eventY=" + mEventY);
                invalidate();
                return false;
            }
        };
//        setOnTouchListener(onTouchListener);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); // 空方法

        canvas.drawCircle(mEventX, mEventY, mRadius, mPaint);
    }

    public void setEventX(float eventX) {
        mEventX = eventX;
    }

    public void setEventY(float eventY) {
        mEventY = eventY;
    }

    public int getRadius() {
        return mRadius;
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        Log.d(TAG, "setOnTouchListener: ");
        super.setOnTouchListener(l);
    }

}
