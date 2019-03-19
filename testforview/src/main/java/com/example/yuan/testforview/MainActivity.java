package com.example.yuan.testforview;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button button;
    private ParentView parentView;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        button = (Button) findViewById(R.id.button);
        parentView = (ParentView) findViewById(R.id.parentView);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //对parentView的内容进行滑动
                parentView.smoothScrollTo();
                testGet();
            }
        });
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MatrixActivity.class));
            }
        });
//        findViewById(R.id.button3).performClick();
        findViewById(R.id.button8).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MyFragmentActivity.class));
            }
        });
    }

    void testGet() {
        Log.d(TAG, "testGet: getX=" + button.getX());
        Log.d(TAG, "testGet: getScrollX=" + button.getScrollX());
        Log.d(TAG, "testGet: getTranslationX=" + button.getTranslationX());
        Log.d(TAG, "testGet: getLeft=" + button.getLeft());
        Log.d(TAG, "testGet: getPaddingLeft=" + button.getPaddingLeft());
        // getScrollX表示View中内容的滑动，View本身不动
        button.scrollBy(5, 10);
        // 改变的是translationX和translationY值，而不是初始的Top、Left等值
//        ObjectAnimator.ofFloat(button, "TranslationX", 0, 200).setDuration(500).start();
        // 改变Left等值,还可缩放
        button.layout(button.getLeft() + 20, button.getTop(), button.getRight() + 50, button.getBottom());
        Log.i(TAG, "testGet: ");
        Log.d(TAG, "testGet: getX=" + button.getX());
        Log.d(TAG, "testGet: getScrollX=" + button.getScrollX());
        Log.d(TAG, "testGet: getTranslationX=" + button.getTranslationX());
        Log.d(TAG, "testGet: getLeft=" + button.getLeft());
        Log.d(TAG, "testGet: getPaddingLeft=" + button.getPaddingLeft());
        Log.e(TAG, "testGet: ");
    }
}
