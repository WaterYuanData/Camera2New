package com.example.camera2video;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        getSupportFragmentManager().beginTransaction().replace(R.id.camera_fragment, new CameraFragment()).commit();
    }
}
