package com.example.test1;

import android.content.Intent;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.aarlib.AarUtils;
import com.example.aarlib.DocumentsUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "yyyy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AarUtils.log(getApplicationContext());

        DocumentsUtils.init(getApplicationContext());

        if (DocumentsUtils.checkWritableRootPath(this, DocumentsUtils.getRootPath())) {
            showOpenDocumentTree();
        }
    }

    private void showOpenDocumentTree() {
        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            StorageManager sm = getSystemService(StorageManager.class);
            StorageVolume volume = sm.getStorageVolume(new File(DocumentsUtils.getRootPath()));
            if (volume != null) {
                intent = volume.createAccessIntent(null);
            }
        }
        if (intent == null) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        }
        startActivityForResult(intent, DocumentsUtils.OPEN_DOCUMENT_TREE_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case DocumentsUtils.OPEN_DOCUMENT_TREE_CODE:
                if (data != null && data.getData() != null) {
                    Uri uri = data.getData();
                    DocumentsUtils.saveTreeUri(this, DocumentsUtils.getRootPath(), uri);
                }
                break;
            default:
                break;
        }
    }

}
