package com.example.yuan.testbeta;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // boolean log_debug = BuildConfig.LOG_DEBUG;
        // Log.d(TAG, "onCreate: log_debug="+log_debug);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                test();
            }
        });
    }

    private void test() {
        try {
            PackageManager packageManager = getApplicationContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo("com.example.yuan.testbeta", 0);
            Toast.makeText(getApplicationContext(), "版本名来自 gradle.properties 为：" + packageInfo.versionName, Toast.LENGTH_SHORT).show();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
