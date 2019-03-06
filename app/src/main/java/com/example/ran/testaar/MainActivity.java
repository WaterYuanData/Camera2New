package com.example.ran.testaar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.cloudminds.storage.DocumentsUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
        Log.d(TAG, "getDisplayRotation: 状态栏的方向 " + rotation);
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

    public void testRead(){
        String sdRootPath = DocumentsUtils.getSdRootPath();
        String s1 = sdRootPath + "/testRead/Folder";
        String s2 = sdRootPath + "/testRead/2.txt";
        // todo getOutputStream有一个误操作会把文件夹当文件生成
        DocumentFile documentFile = DocumentsUtils.getDocumentFile(new File(s1), true);
        OutputStream outputStream = DocumentsUtils.getOutputStream(s1);
        DocumentFile documentFile2 = DocumentsUtils.getDocumentFile(new File(s2), false);
        OutputStream outputStream2 = DocumentsUtils.getOutputStream(s2);
        String st = "你好 hello";
        byte[] bytes = st.getBytes();
        try {
            outputStream2.write(bytes, (int) documentFile2.length(), bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream2 != null) {
                try {
                    // outputStream2.flush(); 作用???
                    outputStream2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        InputStream inputStream = DocumentsUtils.getInputStream(new File(s2));
        try {
            inputStream.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        testRead();
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
