package com.cloudminds.sdcardsaf;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DocumentsUtils {

    private static final String TAG = DocumentsUtils.class.getSimpleName();

    public static final long UNAVAILABLE = -1L;

    public static final int OPEN_DOCUMENT_TREE_CODE = 8000;

    private static List<String> sExtSdCardPaths = new ArrayList<>();

    private static String mRootPath = "";

    private static File mCameraDir = null;

    private static Context mContext;

    private DocumentsUtils() {

    }

    /**
     * 在主Activity的onCreate方法中调用
     *
     * @param context 传入getApplicationContext()即可
     */
    public static void init(Context context) {
        mContext = context;
        setRootPath(mContext);
    }

    public static String getSdRootPath() {
        if (mRootPath == null) {
            Log.e(TAG, "getRootPath: is null");
        }
        return mRootPath;
    }

    public static String getSdCameraDirectory() {
        String cameraDirectory = "";
        if (mCameraDir == null) {
            Log.e(TAG, "getCameraDirectory: is null");
        }
        try {
            cameraDirectory = mCameraDir.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "getCameraDirectory: error");
        }
        return cameraDirectory;
    }

    public static long getSDAvailableSpace() {
        try {
            StatFs stat = new StatFs(mRootPath);
            return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to access external storage", e);
        }
        return DocumentsUtils.UNAVAILABLE;
    }

    public static boolean isMounted() {
        return !TextUtils.isEmpty(mRootPath);
    }

    public static boolean isSdCameraDirAvailable() {
        boolean isCameraDirAvailable = false;
        if (isMounted() && (mCameraDir.isDirectory() || mkdirs(mContext, mCameraDir))) {
            isCameraDirAvailable = canWrite(mCameraDir);
        }
        return isCameraDirAvailable;
    }

    public static String getSdCardFileSystemType() {
        String resultString = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            StorageManager sm = mContext.getSystemService(StorageManager.class);
            StorageVolume volume = sm.getStorageVolume(new File(mRootPath));
            String sdCardUuid = volume != null ? volume.getUuid() : null;
            Log.i(TAG, "getFileSystemType: sdCardUuid=" + sdCardUuid);
            if (sdCardUuid != null) {
                Process mount = null;
                BufferedReader reader = null;
                try {
                    mount = Runtime.getRuntime().exec("mount");
                    reader = new BufferedReader(new InputStreamReader(mount.getInputStream()));
                    mount.waitFor();
                    String line;
                    boolean isResult = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(sdCardUuid)) {
                            Log.i(TAG, "getFileSystemType: line=" + line);
                        }
                        if (!isResult) {
                            String[] split = line.split("\\s+");
                            int length = split.length;
                            for (int i = 0; i < length; i++) {
                                if (split[i].endsWith(sdCardUuid) && (i + 2 < length) && !split[i + 2].contains(sdCardUuid)) {
                                    isResult = true;
                                    resultString = split[i + 2];
                                }
                            }
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (mount != null) {
                        mount.destroy();
                    }
                }
            }
        }
        return resultString;
    }

    public static void setRootPath(Context context) {
        File[] externals = context.getExternalFilesDirs("external");
        for (int i = 0; i < externals.length; i++) {
            try {
                String canonicalPath = externals[i].getCanonicalPath();
                Log.i(TAG, "getRootPath: canonicalPath=" + canonicalPath);
                if (!canonicalPath.contains("emulated")) {
                    int indexOf = canonicalPath.indexOf("/Android/data/");
                    mRootPath = canonicalPath.substring(0, indexOf);
                    mCameraDir = new File(mRootPath + "/DCIM/Camera");
                    Log.i(TAG, "getRootPath: mRootPath=" + mRootPath);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "getRootPath: failed");
            }
        }
    }

    /**
     * 插拔SD卡的接收器中调用该方法
     */
    public static void cleanCache() {
        sExtSdCardPaths.clear();
        mRootPath = "";
        mCameraDir = null;
        setRootPath(mContext);
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String[] getExtSdCardPaths(Context context) {
        if (sExtSdCardPaths.size() > 0) {
            return sExtSdCardPaths.toArray(new String[0]);
        }
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w(TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    sExtSdCardPaths.add(path);
                }
            }
        }
        if (sExtSdCardPaths.isEmpty()) sExtSdCardPaths.add("/storage/sdcard1");
        return sExtSdCardPaths.toArray(new String[0]);
    }

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD
     * card. Otherwise,
     * null is returned.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getExtSdCardFolder(final File file, Context context) {
        String[] extSdPaths = getExtSdCardPaths(context);
        try {
            for (int i = 0; i < extSdPaths.length; i++) {
                if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
                    return extSdPaths[i];
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public static boolean isOnSdCard(final File file) {
        return isOnExtSdCard(file, mContext);
    }

    public static boolean isOnSdCard(final String file) {
        return isOnExtSdCard(new File(file), mContext);
    }

    /**
     * Determine if a file is on external sd card. (Kitkat or higher.)
     *
     * @param file The file.
     * @return true if on external sd card.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isOnExtSdCard(final File file, Context c) {
        return getExtSdCardFolder(file, c) != null;
    }

    public static DocumentFile getDocumentFile(String file, final boolean isDirectory, String mimeType) {
        return getDocumentFile(new File(file), isDirectory, mContext, mimeType);
    }

    public static DocumentFile getDocumentFile(final File file, final boolean isDirectory, String mimeType) {
        return getDocumentFile(file, isDirectory, mContext, mimeType);
    }

    public static DocumentFile getDocumentFile(final File file, final boolean isDirectory,
                                               Context context, String mimeType) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return DocumentFile.fromFile(file);
        }
        String baseFolder = getExtSdCardFolder(file, context);
        boolean originalDirectory = false;
        if (baseFolder == null) {
            return null;
        }
        String relativePath = null;
        try {
            String fullPath = file.getCanonicalPath();
            if (!baseFolder.equals(fullPath)) {
                relativePath = fullPath.substring(baseFolder.length() + 1);
            } else {
                originalDirectory = true;
            }
        } catch (IOException e) {
            return null;
        } catch (Exception f) {
            originalDirectory = true;
            //continue
        }
        String as = PreferenceManager.getDefaultSharedPreferences(context).getString(baseFolder,
                null);
        Uri treeUri = null;
        if (as != null) treeUri = Uri.parse(as);
        if (treeUri == null) {
            return null;
        }
        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);
        if (originalDirectory) return document;
        String[] parts = relativePath.split("/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);
            if (nextDocument == null) {
                if ((i < parts.length - 1) || isDirectory) {
                    nextDocument = document.createDirectory(parts[i]);
                } else {
                    nextDocument = document.createFile(mimeType, parts[i]);
                }
            }
            document = nextDocument;
        }
        return document;
    }

    /**
     * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5).
     * If the file is not
     * existing, it is created.
     *
     * @param file        The file.
     * @param isDirectory flag indicating if the file should be a directory.
     * @return The DocumentFile
     */
    public static DocumentFile getDocumentFile(final File file, final boolean isDirectory) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return DocumentFile.fromFile(file);
        }
        String baseFolder = getExtSdCardFolder(file, mContext);
        boolean originalDirectory = false;
        if (baseFolder == null) {
            return null;
        }
        String relativePath = null;
        try {
            String fullPath = file.getCanonicalPath();
            if (!baseFolder.equals(fullPath)) {
                relativePath = fullPath.substring(baseFolder.length() + 1);
            } else {
                originalDirectory = true;
            }
        } catch (IOException e) {
            return null;
        } catch (Exception f) {
            originalDirectory = true;
            //continue
        }
        String as = PreferenceManager.getDefaultSharedPreferences(mContext).getString(baseFolder,
                null);
        Uri treeUri = null;
        if (as != null) treeUri = Uri.parse(as);
        if (treeUri == null) {
            return null;
        }
        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(mContext, treeUri);
        if (originalDirectory) return document;
        String[] parts = relativePath.split("/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);
            if (nextDocument == null) {
                if ((i < parts.length - 1) || isDirectory) {
                    nextDocument = document.createDirectory(parts[i]);
                } else {
                    nextDocument = document.createFile("image", parts[i]);
                }
            }
            document = nextDocument;
        }
        return document;
    }

    public static boolean mkdirs(Context context, File dir) {
        boolean res = dir.mkdirs();
        if (!res) {
            if (DocumentsUtils.isOnExtSdCard(dir, context)) {
                DocumentFile documentFile = DocumentsUtils.getDocumentFile(dir, true);
                res = documentFile != null && documentFile.canWrite();
            }
        }
        return res;
    }

    public static boolean delete(Context context, File file) {
        boolean ret = file.delete();
        if (!ret && DocumentsUtils.isOnExtSdCard(file, context)) {
            DocumentFile f = DocumentsUtils.getDocumentFile(file, false);
            if (f != null) {
                ret = f.delete();
            }
        }
        return ret;
    }

    /**
     * 兼容DocumentFile
     */
    public static boolean canWrite(File file) {
        boolean res = file.exists() && file.canWrite();
        if (!res && !file.exists()) {
            try {
                if (!file.isDirectory()) {
                    res = file.createNewFile() && file.delete();
                } else {
                    res = file.mkdirs() && file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!res && isOnSdCard(file)) {
            DocumentFile documentFile = getDocumentFile(file, true);
            res = documentFile != null && documentFile.canWrite();
        }
        return res;
    }

    public static boolean renameTo(Context context, File src, File dest) {
        boolean res = src.renameTo(dest);
        if (!res && isOnExtSdCard(dest, context)) {
            DocumentFile srcDoc;
            if (isOnExtSdCard(src, context)) {
                srcDoc = getDocumentFile(src, false);
            } else {
                srcDoc = DocumentFile.fromFile(src);
            }
            DocumentFile destDoc = getDocumentFile(dest.getParentFile(), true);
            if (srcDoc != null && destDoc != null) {
                try {
                    if (src.getParent().equals(dest.getParent())) {
                        res = srcDoc.renameTo(dest.getName());
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        res = DocumentsContract.moveDocument(context.getContentResolver(),
                                srcDoc.getUri(),
                                srcDoc.getParentFile().getUri(),
                                destDoc.getUri()) != null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return res;
    }

    public static InputStream getInputStream(Context context, File destFile) {
        InputStream in = null;
        try {
            if (isOnExtSdCard(destFile, context)) {
                DocumentFile file = DocumentsUtils.getDocumentFile(destFile, false);
                if (file != null && file.canWrite()) {
                    in = context.getContentResolver().openInputStream(file.getUri());
                }
            } else {
                in = new FileInputStream(destFile);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return in;
    }

    public static OutputStream getOutputStream(String destFile) {
        return getOutputStream(new File(destFile));
    }

    public static OutputStream getOutputStream(File destFile) {
        OutputStream out = null;
        try {
            if (isOnSdCard(destFile)) {
                DocumentFile file = getDocumentFile(destFile, false);
                if (file != null && file.canWrite()) {
                    out = mContext.getContentResolver().openOutputStream(file.getUri());
                }
            } else {
                out = new FileOutputStream(destFile);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static boolean saveTreeUri(Context context, String rootPath, Uri uri) {
        DocumentFile file = DocumentFile.fromTreeUri(context, uri);
        if (file != null && file.canWrite()) {
            SharedPreferences perf = PreferenceManager.getDefaultSharedPreferences(context);
            perf.edit().putString(rootPath, uri.toString()).apply();
            try {
                // 为了下次不用反复请求权限的问题,保存授权在系统了，即使reboot了也依然存在，除非clear APP data.
                context.getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                Log.e(TAG, "onActivityResult: 不可保存的授权 " + uri);
                e.printStackTrace();
            }
            return true;
        } else {
            Log.e(TAG, "no write permission: " + rootPath);
        }
        return false;
    }

    public static boolean checkWritableRootPath() {
        return checkWritableRootPath(mContext, mRootPath);
    }

    /**
     * @return true if not writable
     */
    public static boolean checkWritableRootPath(Context context, String rootPath) {
        File root = new File(rootPath);
        if (!TextUtils.isEmpty(rootPath) && !root.canWrite()) {
            Log.d(TAG, "checkWritableRootPath: ");
            if (DocumentsUtils.isOnExtSdCard(root, context)) {
                DocumentFile documentFile = DocumentsUtils.getDocumentFile(root, true);
                return documentFile == null || !documentFile.canWrite();
            } else {
                SharedPreferences perf = PreferenceManager.getDefaultSharedPreferences(context);
                String documentUri = perf.getString(rootPath, "");
                if (documentUri == null || documentUri.isEmpty()) {
                    return true;
                } else {
                    DocumentFile file = DocumentFile.fromTreeUri(context, Uri.parse(documentUri));
                    return !(file != null && file.canWrite());
                }
            }
        }
        return false;
    }
}