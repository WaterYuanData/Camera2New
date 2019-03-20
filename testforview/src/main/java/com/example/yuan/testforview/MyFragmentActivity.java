package com.example.yuan.testforview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MyFragmentActivity extends AppCompatActivity {
    private static final String TAG = "MyFragmentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_my_fragment);
        Log.i(TAG, "onCreate: ------");
        MyFragment myFragment = new MyFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment2, myFragment).commit();
        Log.d(TAG, "onCreate: ++++++");
    }
    /*
    只静态加载
03-20 16:10:36.561 28123-28123/com.example.yuan.testforview D/MyFragmentActivity: onCreate:
03-20 16:10:36.653 28123-28123/com.example.yuan.testforview D/MyFragment: onAttach:
    onCreate:
03-20 16:10:36.654 28123-28123/com.example.yuan.testforview D/MyFragment: onCreateView:
03-20 16:10:36.661 28123-28123/com.example.yuan.testforview D/MyFragment: onViewCreated:
03-20 16:10:36.661 28123-28123/com.example.yuan.testforview I/MyFragmentActivity: onCreate: ------
03-20 16:10:36.661 28123-28123/com.example.yuan.testforview D/MyFragmentActivity: onCreate: ++++++
03-20 16:10:36.664 28123-28123/com.example.yuan.testforview D/MyFragment: onActivityCreated:
    onStart:
03-20 16:10:36.667 28123-28123/com.example.yuan.testforview D/MyFragment: onResume:

    只动态加载
03-20 16:08:20.691 27953-27953/com.example.yuan.testforview D/MyFragmentActivity: onCreate:
03-20 16:08:20.739 27953-27953/com.example.yuan.testforview I/MyFragmentActivity: onCreate: ------
03-20 16:08:20.755 27953-27953/com.example.yuan.testforview D/MyFragmentActivity: onCreate: ++++++
03-20 16:08:20.765 27953-27953/com.example.yuan.testforview D/MyFragment: onAttach:
    onCreate:
03-20 16:08:20.766 27953-27953/com.example.yuan.testforview D/MyFragment: onCreateView:
03-20 16:08:20.772 27953-27953/com.example.yuan.testforview D/MyFragment: onViewCreated:
    onActivityCreated:
    onStart:
03-20 16:08:20.774 27953-27953/com.example.yuan.testforview D/MyFragment: onResume:
    */
}
