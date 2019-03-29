package com.example.yuan.camera2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ShutterButton extends View {
    private static final String TAG = "ShutterButton";
    private Paint mRedPaint;
    private Paint mWhitePaint;
    boolean mStartAnimation = false;
    private float mStartAngle = 270; // 起始位置开始画
    private int mSweepAngle = 0;
    private long mDelayMillis = 1000L / 24; // 24fps
    private int mTime = 3 * 1000; // 周期
    private int mOffSet = 10;
    private long mStartTime;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            postInvalidate();
        }
    };

    public ShutterButton(Context context) {
        super(context);
    }

    public ShutterButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mRedPaint = new Paint();
        mRedPaint.setColor(Color.RED);
        mWhitePaint = new Paint();
        mWhitePaint.setColor(Color.WHITE);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return super.onTouchEvent(event);
    }

    public void startAnimation() {
        mStartTime = System.currentTimeMillis();
        mSweepAngle = 0;
        mStartAnimation = true;
        postInvalidate();
    }

    public void stopAnimation() {
        Log.d(TAG, "stopAnimation: " + (System.currentTimeMillis() - mStartTime));
        mSweepAngle = 0;
        mStartAnimation = false;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2, mRedPaint);
        if (mStartAnimation) {
            long currentTimeMillis = System.currentTimeMillis();
            canvas.drawArc(0, 0, getWidth(), getHeight(), mStartAngle, mSweepAngle, true, mWhitePaint);
            mSweepAngle = (mSweepAngle + mOffSet) % 360;
            if (mSweepAngle == 0) {
                stopAnimation();
            }
            postDelayed(mRunnable, 1000 / 24);// 24fps的速率刷新，避免频繁调用UI线程
        }
    }
}
