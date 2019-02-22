package com.example.yuan.testforview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ViewA extends View {
    private final int radius = 50;

    public int getRadius() {
        return radius;
    }

    public ViewA(Context context) {
        super(context);
    }

    public ViewA(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /**
         * 1、先实例化一个Paint对象，该对象充当“画笔”的作用
         * 2、设置抗锯齿、画笔颜色等，这里填充为蓝色
         * 3、调用canvas的drawCircle方法绘制圆形，
         *    第1、2个参数表示坐标，第3个参数表示半径
         */
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        canvas.drawCircle(50, 50, radius, paint);
    }
}
