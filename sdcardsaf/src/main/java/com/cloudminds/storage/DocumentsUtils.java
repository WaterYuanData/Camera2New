package com.cloudminds.storage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * 通过 存储访问框架(SAF) 访问外置SD卡
 */
public class DocumentsUtils {

    private static final String TAG = DocumentsUtils.class.getSimpleName();

    /**
     * 授权请求的requestCode
     */
    public static final int OPEN_DOCUMENT_TREE_CODE = 8000;

    private static final long UNAVAILABLE = -1L;

    private static String mRootPath = "";

    private static Context mContext;

    private DocumentsUtils() {

    }

    /**
     * 初始化DocumentsUtils工具类,一般在主Activity的onCreate方法中调用即可
     *
     * @param context 一般传入getApplicationContext()即可
     */
    public static void init(Context context) {
        mContext = context.getApplicationContext();
        reInit();
    }

    /**
     * 必须在插拔SD卡的广播接收器中调用该方法
     */
    public static void reInit() {
        mRootPath = "";
        File[] externals = mContext.getExternalFilesDirs("external");
        for (File external : externals) {
            try {
                if (external == null) {
                    Log.e(TAG, "setRootPath: null");
                    break;
                }
                String canonicalPath = external.getCanonicalPath();
                Log.i(TAG, "setRootPath: canonicalPath=" + canonicalPath);
                if (!canonicalPath.contains("emulated")) {
                    int indexOf = canonicalPath.indexOf("/Android/data/");
                    mRootPath = canonicalPath.substring(0, indexOf);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "setRootPath: failed");
            }
        }
    }

    /**
     * 获取SD卡的规范化全路径
     */
    public static String getSdRootPath() {
        if (mRootPath == null) {
            Log.e(TAG, "getRootPath: is null");
        }
        return mRootPath;
    }

    /**
     * 判断SD卡是否已有效加载
     */
    public static boolean isMounted() {
        return !TextUtils.isEmpty(mRootPath);
    }

    /**
     * 获取SD卡可用空间
     */
    public static long getSDAvailableSpace() {
        if (TextUtils.isEmpty(mRootPath)) {
            return UNAVAILABLE;
        }
        try {
            StatFs stat = new StatFs(mRootPath);
            return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to access external storage", e);
        }
        return UNAVAILABLE;
    }

    /**
     * 获取SD卡文件系统的类型,即SD卡的格式
     */
    public static String getSdCardFileSystemType() {
        String resultString = "";
        StorageManager sm = mContext.getSystemService(StorageManager.class);
        if (sm == null) {
            return resultString;
        }
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
        return resultString;
    }

    /**
     * 判断一个文件路径是否在SD卡上
     */
    public static boolean isOnSdCard(String file) {
        return isOnSdCard(new File(file));
    }

    /**
     * 判断一个文件是否在SD卡上
     */
    public static boolean isOnSdCard(File file) {
        return getExtSdCardFolder(file) != null;
    }

    /**
     * 获取一个文件在SD卡上的跟目录,如果不在SD上卡返回null
     */
    private static String getExtSdCardFolder(File file) {
        if (!TextUtils.isEmpty(mRootPath)) {
            try {
                if (file.getCanonicalPath().startsWith(mRootPath)) {
                    return mRootPath;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * 获取file对应的DocumentFile,如果file不存在会根据isDirectory自动创建文件或者文件夹
     *
     * @param file        The file.
     * @param isDirectory 指明要得到的DocumentFile是文件还是文件夹
     * @return The DocumentFile
     */
    public static DocumentFile getDocumentFile(File file, boolean isDirectory) {
        return getDocumentFile(file, isDirectory, "");
    }

    /**
     * 获取file对应的DocumentFile,如果file不存在会根据isDirectory自动创建文件或者文件夹
     *
     * @param file        The file.
     * @param isDirectory 指明要得到的DocumentFile是文件还是文件夹
     * @param mimeType    文件的文件类型
     * @return The DocumentFile
     */
    public static DocumentFile getDocumentFile(File file, boolean isDirectory, String mimeType) {
        String baseFolder = getExtSdCardFolder(file);
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
        String as = PreferenceManager.getDefaultSharedPreferences(mContext).getString(baseFolder, null);
        Uri treeUri = null;
        if (as != null) {
            treeUri = Uri.parse(as);
        }
        if (treeUri == null) {
            return null;
        }
        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(mContext, treeUri);
        if (originalDirectory) {
            return document;
        }
        String[] parts = relativePath.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (document == null) {
                return null;
            }
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
     * 兼容DocumentFile的创建文件夹
     */
    public static boolean mkdirs(File dir) {
        boolean res = dir.mkdirs();
        if (!res) {
            if (isOnSdCard(dir)) {
                DocumentFile documentFile = getDocumentFile(dir, true);
                res = documentFile != null && documentFile.canWrite();
            }
        }
        return res;
    }

    /**
     * 兼容DocumentFile的删除文件/文件夹
     */
    public static boolean delete(File file, boolean isDirectory) {
        boolean ret = file.delete();
        if (!ret && isOnSdCard(file)) {
            DocumentFile f = getDocumentFile(file, isDirectory);
            if (f != null) {
                ret = f.delete();
            }
        }
        return ret;
    }

    /**
     * 不借助DocumentFile的条件下,判断文件是否可写
     */
    public static boolean canWriteNotByDoc(File file) {
        boolean res = file.exists() && file.canWrite();
        if (!res && !file.exists()) {
            try {
                if (!file.isDirectory()) {
                    res = file.createNewFile() && file.delete();
                } else {
                    res = file.mkdirs() && file.delete();
                }
            } catch (IOException e) {
                // e.printStackTrace();
                Log.i(TAG, "canNotWriteNotByDoc: " + file.getName());
            }
        }
        return res;
    }

    /**
     * 借助DocumentFile的条件下,判断文件/文件夹是否可写
     */
    public static boolean canWriteByDoc(File file, boolean isDirectory) {
        boolean res = canWriteNotByDoc(file);
        if (!res && isOnSdCard(file)) {
            DocumentFile documentFile = getDocumentFile(file, isDirectory);
            res = documentFile != null && documentFile.canWrite();
        }
        return res;
    }

    /**
     * 兼容DocumentFile的重命名文件/文件夹
     *
     * @param src         重命名前的源文件
     * @param dest        重命名后的目标文件
     * @param isDirectory 指明文件/文件夹
     */
    public static boolean renameTo(File src, File dest, boolean isDirectory) {
        boolean res = src.renameTo(dest);
        if (!res && isOnSdCard(dest)) {
            DocumentFile srcDoc;
            if (isOnSdCard(src)) {
                srcDoc = getDocumentFile(src, isDirectory);
            } else {
                srcDoc = DocumentFile.fromFile(src);
            }
            DocumentFile destDoc = getDocumentFile(dest.getParentFile(), isDirectory);
            if (srcDoc != null && destDoc != null) {
                try {
                    if (src.getParent().equals(dest.getParent())) {
                        res = srcDoc.renameTo(dest.getName());
                    } else {
                        res = DocumentsContract.moveDocument(mContext.getContentResolver(),
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

    /**
     * 获取文件的FileInputStream
     * 会根据 !canWriteNotByDoc && isOnSdCard 自动判断:用DocumentFile形式还是非DocumentFile形式
     */
    public static InputStream getInputStream(File destFile) {
        InputStream in = null;
        try {
            if (!canWriteNotByDoc(destFile) && isOnSdCard(destFile)) {
                DocumentFile file = getDocumentFile(destFile, false);
                if (file != null && file.canWrite()) {
                    in = mContext.getContentResolver().openInputStream(file.getUri());
                }
            } else {
                in = new FileInputStream(destFile);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return in;
    }

    /**
     * 获取一个文件的FileDescriptor,一般用于MediaRecorder.setOutputFile
     * 注意:调用前务必进行 !canWriteNotByDoc && isOnSdCard 判断,满足条件才能调用
     */
    public static FileDescriptor getFileDescriptor(String filePath) {
        return getFileDescriptor(new File(filePath));
    }


    /**
     * 获取一个文件的FileDescriptor,一般用于MediaRecorder.setOutputFile
     * 注意:调用前务必进行 !canWriteNotByDoc && isOnSdCard 判断,满足条件才能调用
     */
    public static FileDescriptor getFileDescriptor(File destFile) {
        FileDescriptor fileDescriptor = null;
        Log.i(TAG, "getFileDescriptor: " + destFile.toString());
        try {
            if (!canWriteNotByDoc(destFile) && isOnSdCard(destFile)) {
                DocumentFile file = getDocumentFile(destFile, false);
                if (file != null && file.canWrite()) {
                    fileDescriptor = mContext.getContentResolver().openFileDescriptor(file.getUri(), "rw").getFileDescriptor();
                }
            } else {
                Log.e(TAG, "不满足调用 getFileDescriptor 的条件");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getFileDescriptor: Failed");
        }
        return fileDescriptor;
    }

    /**
     * 获取一个文件的FileOutputStream
     * 会根据 !canWriteNotByDoc && isOnSdCard 自动判断:用DocumentFile形式还是非DocumentFile形式
     */
    public static OutputStream getOutputStream(String destFile) {
        return getOutputStream(new File(destFile));
    }

    /**
     * 获取一个文件的FileOutputStream
     * 会根据 !canWriteNotByDoc && isOnSdCard 自动判断:用DocumentFile形式还是非DocumentFile形式
     */
    public static OutputStream getOutputStream(File destFile) {
        OutputStream out = null;
        Log.i(TAG, "getOutputStream: " + destFile.toString());
        try {
            if (!canWriteNotByDoc(destFile) && isOnSdCard(destFile)) {
                DocumentFile file = getDocumentFile(destFile, false);
                if (file != null && file.canWrite()) {
                    out = mContext.getContentResolver().openOutputStream(file.getUri());
                }
            } else {
                out = new FileOutputStream(destFile);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "getOutputStream: Failed");
        }
        return out;
    }

    /**
     * 保存授权框返回的Uri
     */
    public static boolean saveTreeUri(Uri uri) {
        DocumentFile file = DocumentFile.fromTreeUri(mContext, uri);
        if (file != null && file.canWrite()) {
            SharedPreferences perf = PreferenceManager.getDefaultSharedPreferences(mContext);
            perf.edit().putString(mRootPath, uri.toString()).apply();
            try {
                // 为了下次不用反复请求权限的问题,保存授权在系统了，即使reboot了也依然存在，除非clear APP data.
                mContext.getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                Log.e(TAG, "onActivityResult: 不可保存的授权 " + uri);
                e.printStackTrace();
            }
            return true;
        } else {
            Log.e(TAG, "no write permission: " + mRootPath);
        }
        return false;
    }

    /**
     * 判断是否需要申请SD卡写权限,已兼容DocumentFile
     */
    public static boolean isNeedRequestWriteSDRootPath() {
        Log.i(TAG, "isNeedRequestWriteSDRootPath: " + mRootPath);
        if (TextUtils.isEmpty(mRootPath)) {
            return false;
        }
        File root = new File(mRootPath);
        if (!root.canWrite()) {
            DocumentFile documentFile = getDocumentFile(root, true);
            return documentFile == null || !documentFile.canWrite();
        }
        return false;
    }
}