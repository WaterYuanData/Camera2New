package com.example.yuan.testforview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MyFragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_fragment);

        MyFragment myFragment = new MyFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment2, myFragment).commit();
    }
}
