package com.example.ran.testaar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "yyyy";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        boolean log_debug = BuildConfig.LOG_E;
//        Log.d(TAG, "onCreate: log_debug="+log_debug);
    }
}
