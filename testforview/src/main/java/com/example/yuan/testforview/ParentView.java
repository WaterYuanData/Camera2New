package com.example.yuan.testforview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.BounceInterpolator;
import android.widget.LinearLayout;
import android.widget.Scroller;

public class ParentView extends LinearLayout {

    private static final String TAG = "ParentView";
    private Scroller mScroller;
    private ViewA viewA;
    private int realHeight;

    public ParentView(Context context) {
        super(context);
    }

    public ParentView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //为了实现回弹效果，这里传递一个BounceInterpolator插值器，该插值器专门用于实现回弹效果
        mScroller = new Scroller(context, new BounceInterpolator());
    }

    /**
     * 初始化ScrollX、ScrollY,同时获取子View的实例，获取其半径参数
     * startScroll(int startX, int startY, int dx, int dy, int duration)方法：
     * startX、startY表示滑动开始的坐标；dx、dy表示需要位移的距离；duration表示移位的时间
     * invalidate()方法：在View树重绘的时候会调用computeScrollOffset()方法
     */
    public void smoothScrollTo() {
        Log.d(TAG, "testGet: getX=" + getX());
        Log.d(TAG, "testGet: getScrollX=" + getScrollX());
        Log.d(TAG, "testGet: getTranslationX=" + getTranslationX());
        Log.d(TAG, "testGet: getLeft=" + getLeft());
        Log.d(TAG, "testGet: getPaddingLeft=" + getPaddingLeft());
        Log.d(TAG, "testGet: getY=" + getY());
        Log.d(TAG, "testGet: getScrollY=" + getScrollY());
        Log.d(TAG, "testGet: getTranslationY=" + getTranslationY());
        Log.d(TAG, "testGet: getRight=" + getRight());
        Log.d(TAG, "testGet: getPaddingRight=" + getPaddingRight());
        Log.d(TAG, "testGet: getWidth=" + getWidth());
        Log.d(TAG, "testGet: getHeight=" + getHeight());
        Log.d(TAG, "testGet: getBottom=" + getBottom());
        viewA = (ViewA) getChildAt(0);
        int ScrollX = getScrollX();
        int ScrollY = getScrollY();
        realHeight = getHeight() - 2 * viewA.getRadius();
        mScroller.startScroll(ScrollX, 0, 0, -realHeight, 1000);
        invalidate();
    }

    /**
     * 先调用computeScrollOffset()方法，计算出新的CurrX和CurrY值，
     * 判断是否需要继续滑动。
     * scrollTo(currX,currY):滑动到上面计算出的新的currX和currY位置处
     * postInvalidate():通知View树重绘，作用和invalidate()方法一样
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int currX = mScroller.getCurrX();
            int currY = mScroller.getCurrY();
            Log.d("cylog", "滑动坐标" + "(" + getScrollX() + "," + getScrollY() + ")");
            scrollTo(currX, currY);
            postInvalidate();
        } else {
            int currX = mScroller.getCurrX();
            int currY = mScroller.getCurrY();
            Log.i("cylog", "滑动坐标" + "(" + getScrollX() + "," + getScrollY() + ")");
        }
    }
}
