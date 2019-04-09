package com.example.yuan.camera2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class LoadingRender extends View {
    private static final String TAG = "LoadingRender";
    private ValueAnimator mRenderAnimator;
    private static final long ANIMATION_DURATION = 1333;

    private final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private final Interpolator MATERIAL_INTERPOLATOR = new FastOutSlowInInterpolator();
    private final Interpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final int NUM_POINTS = 5;
    private static final int DEGREE_360 = 360;
    private final float MAX_SWIPE_DEGREES = 0.8f * DEGREE_360;
    private final float FULL_GROUP_ROTATION = 3.0f * DEGREE_360;
    private final float[] LEVEL_SWEEP_ANGLE_OFFSETS = new float[]{1.0f, 7.0f / 8.0f, 5.0f / 8.0f};
    private static final float START_TRIM_DURATION_OFFSET = 0.5f;
    private static final float END_TRIM_DURATION_OFFSET = 1.0f;
    private final int[] DEFAULT_LEVEL_COLORS = new int[]{Color.parseColor("#55ffffff"),
            Color.parseColor("#b1ffffff"), Color.parseColor("#ffffffff")};
    private Paint mPaint = new Paint();
    private RectF mTempBounds;
    private float[] mLevelSwipeDegrees = new float[3];
    private float mRotationCount;
    private float mGroupRotation;
    private float mEndDegrees;
    private float mStartDegrees;
    private float mOriginEndDegrees;
    private float mOriginStartDegrees;
    private float mStrokeWidth = 30;
    private float mCenterRadius;

    private final ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            computeRender((float) animation.getAnimatedValue());
            Log.d(TAG, "onAnimationUpdate: " + animation.getAnimatedValue());
            postInvalidate();
        }
    };

    private final Animator.AnimatorListener mAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationRepeat(Animator animator) {
            super.onAnimationRepeat(animator);
            storeOriginals();

            mStartDegrees = mEndDegrees;
            mRotationCount = (mRotationCount + 1) % (NUM_POINTS);
        }

        @Override
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            mRotationCount = 0;
        }
    };

    private void storeOriginals() {
        mOriginEndDegrees = mEndDegrees;
        mOriginStartDegrees = mEndDegrees;
    }

    public LoadingRender(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupAnimators();
        setupPaint();
    }

    private void setupAnimators() {
        mRenderAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);// 与ofFloat(0, 360)的区别???
        mRenderAnimator.setRepeatCount(Animation.INFINITE);// Animation与ValueAnimator的区别
        mRenderAnimator.setRepeatMode(ValueAnimator.RESTART);
        mRenderAnimator.setDuration(ANIMATION_DURATION);
        //fuck you! the default interpolator is AccelerateDecelerateInterpolator
        mRenderAnimator.setInterpolator(new LinearInterpolator());
        mRenderAnimator.addUpdateListener(mAnimatorUpdateListener);
        mRenderAnimator.addListener(mAnimatorListener);
    }

    private void setupPaint() {
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

//    private void initStrokeInset(float width, float height) {
//        float minSize = Math.min(width, height);
//        float strokeInset = minSize / 2.0f - mCenterRadius;
//        float minStrokeInset = (float) Math.ceil(mStrokeWidth / 2.0f);
//        mStrokeInset = strokeInset < minStrokeInset ? minStrokeInset : strokeInset;
//    }

    protected void onDraw(Canvas canvas) {
        // int saveCount = canvas.save();

        if (mCenterRadius <= 0) {
            mCenterRadius = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2;
        }

        if (mTempBounds == null) {
            mTempBounds = new RectF();
            mTempBounds.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            mTempBounds.inset(mStrokeWidth / 2, mStrokeWidth / 2); // 在边沿画线让过笔宽的一半
        }

        canvas.rotate(mGroupRotation, mTempBounds.centerX(), mTempBounds.centerY());

        for (int i = 0; i < 3; i++) {
            if (mLevelSwipeDegrees[i] != 0) {
                mPaint.setColor(DEFAULT_LEVEL_COLORS[i]);
                Log.d(TAG, "onDraw: " + mTempBounds.toShortString() + mGroupRotation + " " + mLevelSwipeDegrees[i] + " " + mEndDegrees + " " + DEFAULT_LEVEL_COLORS[i]);
                canvas.drawArc(mTempBounds, mEndDegrees, mLevelSwipeDegrees[i], false, mPaint);
            }
        }

        // canvas.restoreToCount(saveCount);
    }

    protected void computeRender(float renderProgress) {
        // Moving the start trim only occurs in the first 50% of a single ring animation
        if (renderProgress <= START_TRIM_DURATION_OFFSET) {
            float startTrimProgress = (renderProgress) / START_TRIM_DURATION_OFFSET;
            mStartDegrees = mOriginStartDegrees + MAX_SWIPE_DEGREES * MATERIAL_INTERPOLATOR.getInterpolation(startTrimProgress);

            float mSwipeDegrees = mEndDegrees - mStartDegrees;
            float levelSwipeDegreesProgress = Math.abs(mSwipeDegrees) / MAX_SWIPE_DEGREES;

            float level1Increment = DECELERATE_INTERPOLATOR.getInterpolation(levelSwipeDegreesProgress) - LINEAR_INTERPOLATOR.getInterpolation(levelSwipeDegreesProgress);
            float level3Increment = ACCELERATE_INTERPOLATOR.getInterpolation(levelSwipeDegreesProgress) - LINEAR_INTERPOLATOR.getInterpolation(levelSwipeDegreesProgress);

            mLevelSwipeDegrees[0] = -mSwipeDegrees * LEVEL_SWEEP_ANGLE_OFFSETS[0] * (1.0f + level1Increment);
            mLevelSwipeDegrees[1] = -mSwipeDegrees * LEVEL_SWEEP_ANGLE_OFFSETS[1] * 1.0f;
            mLevelSwipeDegrees[2] = -mSwipeDegrees * LEVEL_SWEEP_ANGLE_OFFSETS[2] * (1.0f + level3Increment);
        }

        // Moving the end trim starts after 50% of a single ring animation
        if (renderProgress > START_TRIM_DURATION_OFFSET) {
            float endTrimProgress = (renderProgress - START_TRIM_DURATION_OFFSET) / (END_TRIM_DURATION_OFFSET - START_TRIM_DURATION_OFFSET);
            mEndDegrees = mOriginEndDegrees + MAX_SWIPE_DEGREES * MATERIAL_INTERPOLATOR.getInterpolation(endTrimProgress);

            float mSwipeDegrees = mEndDegrees - mStartDegrees;
            float levelSwipeDegreesProgress = Math.abs(mSwipeDegrees) / MAX_SWIPE_DEGREES;

            if (levelSwipeDegreesProgress > LEVEL_SWEEP_ANGLE_OFFSETS[1]) {
                mLevelSwipeDegrees[0] = -mSwipeDegrees;
                mLevelSwipeDegrees[1] = MAX_SWIPE_DEGREES * LEVEL_SWEEP_ANGLE_OFFSETS[1];
                mLevelSwipeDegrees[2] = MAX_SWIPE_DEGREES * LEVEL_SWEEP_ANGLE_OFFSETS[2];
            } else if (levelSwipeDegreesProgress > LEVEL_SWEEP_ANGLE_OFFSETS[2]) {
                mLevelSwipeDegrees[0] = 0;
                mLevelSwipeDegrees[1] = -mSwipeDegrees;
                mLevelSwipeDegrees[2] = MAX_SWIPE_DEGREES * LEVEL_SWEEP_ANGLE_OFFSETS[2];
            } else {
                mLevelSwipeDegrees[0] = 0;
                mLevelSwipeDegrees[1] = 0;
                mLevelSwipeDegrees[2] = -mSwipeDegrees;
            }
        }

        mGroupRotation = ((FULL_GROUP_ROTATION / NUM_POINTS) * renderProgress) + (FULL_GROUP_ROTATION * (mRotationCount / NUM_POINTS));
    }

    void start() {
        mRenderAnimator.start();
    }

    void stop() {
        // if I just call mRenderAnimator.end(),
        // it will always call the method onAnimationUpdate(ValueAnimator animation)
        // why ? if you know why please send email to me (dinus_developer@163.com)
        // mRenderAnimator.removeUpdateListener(mAnimatorUpdateListener);
        //
        // mRenderAnimator.setRepeatCount(0);
        // mRenderAnimator.setDuration(0);
        mRenderAnimator.end();
    }

    boolean isRunning() {
        return mRenderAnimator.isRunning();
    }
}
