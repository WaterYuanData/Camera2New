package com.example.ran.testaar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.cloudminds.storage.DocumentsUtils;

import java.io.File;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "yyyy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DocumentsUtils.init(getApplicationContext());
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test();
                testOrientation();
            }
        });
    }

    public void testOrientation() {
        getDisplayRotation(this);
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Log.d(TAG, "getDisplayRotation: 显示方向 " + rotation);
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    public void test() {
        if (DocumentsUtils.isNeedRequestWriteSDRootPath()) {
            Intent intent = null;
            StorageManager sm = getSystemService(StorageManager.class);
            StorageVolume volume = sm.getStorageVolume(new File(DocumentsUtils.getSdRootPath()));
            if (volume != null) {
                intent = volume.createAccessIntent(null);
            }
            if (intent == null) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            }
            startActivityForResult(intent, DocumentsUtils.OPEN_DOCUMENT_TREE_CODE);
            return;
        }

        long sdAvailableSpace = DocumentsUtils.getSDAvailableSpace() / 1024 / 1024;
        Toast.makeText(getApplicationContext(), "" + sdAvailableSpace, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "test: sdAvailableSpace=" + sdAvailableSpace);

        Log.i(TAG, "test: canWrite=" + new File(DocumentsUtils.getSdRootPath()).canWrite());

        String sdRootPath = DocumentsUtils.getSdRootPath();
        String s1 = sdRootPath + "/test/1";
        String s2 = sdRootPath + "/test/2/2.txt";
        String s3 = sdRootPath + "/test/3/";
        String s4 = sdRootPath + "/test/4/4.txt";
        new File(s1).mkdirs();
        new File(s2).mkdirs();
        DocumentsUtils.mkdirs(new File(s3));
        DocumentsUtils.mkdirs(new File(s4));

        testfile();
    }

    void testfile() {
        String sdRootPath = DocumentsUtils.getSdRootPath();
        String s1 = sdRootPath + "/testfile/1";
        String s2 = sdRootPath + "/testfile/1/2.txt";
        DocumentsUtils.getDocumentFile(new File(s1), false);
        OutputStream outputStream = DocumentsUtils.getOutputStream(s1);
        DocumentsUtils.getDocumentFile(new File(s2), false);
        /**
         * getDocumentFile返回值可能为null,会致使getOutputStream也为null
         * 把s1生成文件即可导致s2生成为null
         * 把s1生成文件夹才能使s2不为null
         * */
        outputStream = DocumentsUtils.getOutputStream(s2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DocumentsUtils.OPEN_DOCUMENT_TREE_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null && data.getData() != null) {
                    Uri uri = data.getData();
                    DocumentsUtils.saveTreeUri(uri);
                }
            }
            return;
        }
    }
}
