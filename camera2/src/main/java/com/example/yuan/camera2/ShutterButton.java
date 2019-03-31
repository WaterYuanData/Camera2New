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
    private float mStartAngle = 270; // 起始角度
    private int mSweepAngle; // 当前角度
    /*
     * 以3s为周期画一圈
     * 以每次画30°为例，画一圈需要画12次
     * 3s画12次，则间隔时长 3 * 1000 / 12 画一次
     * */
    private int mCycleTime = 5 * 1000; // 周期
    private int mOffSetAngle = 15; // 每次偏移的角度
    private int mOffSetTimes = 360 / mOffSetAngle; // 一圈的偏移次数
    private long mOffSetDelayMillis = mCycleTime / mOffSetTimes; // 间隔多长时间画一次
    private long mStartTime;
    private long mCurrentTimeMillis;
    private long mLastTimeMillis;
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
        mCurrentTimeMillis = 0;
        mSweepAngle = 0;
        mStartAnimation = true;
        Log.d(TAG, "startAnimation: mOffSetDelayMillis=" + mOffSetDelayMillis);
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
            mLastTimeMillis = mCurrentTimeMillis;
            mCurrentTimeMillis = System.currentTimeMillis();
            mSweepAngle = (int) (360 * (mCurrentTimeMillis - mStartTime) / mCycleTime);
            if (mSweepAngle >= 360) {
                stopAnimation();
                return;
            }
            Log.d(TAG, "onDraw: mSweepAngle=" + mSweepAngle + " mCurrentTimeMillis=" + (mCurrentTimeMillis - mStartTime));
            Log.d(TAG, "onDraw: " + (mCurrentTimeMillis - mLastTimeMillis));
            canvas.drawArc(0, 0, getWidth(), getHeight(), mStartAngle, mSweepAngle, true, mWhitePaint);
            postDelayed(mRunnable, mOffSetDelayMillis);
        }
    }
}
