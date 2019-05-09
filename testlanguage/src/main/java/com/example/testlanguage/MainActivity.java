package com.example.testlanguage;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.testlanguage.util.ViewUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Button button;
    private String localeString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: ");
        Toast.makeText(getApplicationContext(), "重启", Toast.LENGTH_SHORT).show();
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchLanguage();
                button.setText(localeString);
            }
        });

        EventBus.getDefault().register(this);
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TestActivity.class));
            }
        });
    }

    private void switchLanguage() {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        if (localeString != null && localeString.contains("zh")) {
            configuration.setLocale(Locale.ENGLISH);
            localeString = Locale.ENGLISH.toString();
        } else {
            configuration.setLocale(Locale.CHINA);
            localeString = Locale.CHINA.toString();

        }
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
//        getApplicationContext().createConfigurationContext(configuration);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.i(TAG, "onWindowFocusChanged: 焦点=" + hasFocus);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Locale locale = newConfig.getLocales().get(0);
        localeString = locale.toString();
        Log.i(TAG, "onConfigurationChanged: " + locale.toString());
        Toast.makeText(getApplicationContext(), "语言=" + locale.toString(), Toast.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN) //在ui线程执行
    public void onStringEvent(ClassEvent event) {
        Log.d("test", "MainActivity got message:" + event);
        ViewUtil.updateViewLanguage(findViewById(android.R.id.content));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);//反注册EventBus
    }
}
