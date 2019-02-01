package com.example.ran.testaar;

import android.content.Intent;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.cloudminds.sdcardsaf.DocumentsUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "yyyy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // boolean log_debug = BuildConfig.LOG_DEBUG;
        // Log.d(TAG, "onCreate: log_debug="+log_debug);
        DocumentsUtils.init(getApplicationContext());
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test();
            }
        });
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

        String sdRootPath = DocumentsUtils.getSdRootPath();
        String s1 = sdRootPath + "/test/1";
        String s2 = sdRootPath + "/test/2/2.txt";
        String s3 = sdRootPath + "/test/3/";
        String s4 = sdRootPath + "/test/4/4.txt";
        new File(s1).mkdirs();
        new File(s2).mkdirs();
        DocumentsUtils.mkdirs(new File(s3));
        DocumentsUtils.mkdirs(new File(s4));
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
