package org.md2k.datakit.operation;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import org.md2k.utilities.Report.Log;

import java.io.File;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class FileManager {
    public static final String INTERNAL_SDCARD = "INTERNAL_SDCARD";
    public static final String EXTERNAL_SDCARD = "EXTERNAL_SDCARD";
    public static final String EXTERNAL_SDCARD_PREFERRED = "EXTERNAL_SDCARD_PREFERRED";
    public static final String INTERNAL_SDCARD_PREFERRED = "INTERNAL_SDCARD_PREFERRED";
    public static final String NONE = "NONE";

    private static final String TAG = FileManager.class.getSimpleName();


    public static final int VERSION = 1;

    public static String getInternalSDCardDirectory(Context context) {
        String directory = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            directory = Environment.getExternalStorageDirectory().toString() + "/Android/data/" + context.getPackageName() + "/files/";
            File dir = new File(directory);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    directory = null;
                }
            }
        }
        return directory;
    }

    public static String getExternalSDCardDirectory(Context context) {
        String strSDCardPath = System.getenv("SECONDARY_STORAGE");
        File[] externalFilesDirs = context.getExternalFilesDirs(null);
        for (File externalFilesDir : externalFilesDirs) {
            if (externalFilesDir == null) continue;
            if (externalFilesDir.getAbsolutePath().contains(strSDCardPath))
                return externalFilesDir.getAbsolutePath();
        }
        return null;
    }

    public static String getFilePath(Context context) {
        String filepath = getDirectory(context);
        if (filepath != null)
            filepath = filepath + getFileName();
        return filepath;
    }

    public static String getValidSDcard(Context context, String STORAGE_OPTION) {
        switch (STORAGE_OPTION) {
            case INTERNAL_SDCARD:
                if (getInternalSDCardDirectory(context) == null) return "Not found";
                else return INTERNAL_SDCARD_STR;
            case EXTERNAL_SDCARD:
                if (getExternalSDCardDirectory(context) == null) return "Not found";
                else return EXTERNAL_SDCARD_STR;
            case INTERNAL_SDCARD_PREFERRED:
                if (getInternalSDCardDirectory(context) != null) return INTERNAL_SDCARD_STR;
                else if (getExternalSDCardDirectory(context) != null) return EXTERNAL_SDCARD_STR;
                else return "Not found";
            case EXTERNAL_SDCARD_PREFERRED:
                if (getExternalSDCardDirectory(context) != null) return EXTERNAL_SDCARD_STR;
                else if (getInternalSDCardDirectory(context) != null) return INTERNAL_SDCARD_STR;
                else return "Not found";
            case NONE:
                return "none";
            default:
                return "(null)";
        }
    }

    public static String getStorageSpace(Context context) {
        String sdCard = getValidSDcard(context);
        long available=0, total=0, used=0;
        String totalStr = "-", usedStr="-";
        if (sdCard.equals(INTERNAL_SDCARD_STR)) {
            available = getAvailableSDCardSize(Environment.getExternalStorageDirectory());
            total = getTotalSDCardSize(Environment.getExternalStorageDirectory());
            used=total-available;
            usedStr=formatSize(used);
            totalStr=formatSize(total);
        } else if (sdCard.equals(EXTERNAL_SDCARD_STR)) {
            available = getAvailableSDCardSize(getExternalSDCardPath(context));
            total = getTotalSDCardSize(getExternalSDCardPath(context));
            used=total-available;
            totalStr=formatSize(total);
            usedStr=formatSize(used);

        }
        return usedStr + " out of " + totalStr + " ( "+String.valueOf(used*100/total)+"% )";
    }
    public static String getFileSize(Context context){
        long fileSize=getFileSize(new File(getFilePath(context)));
        return formatSize(fileSize);
    }

    public static long getFileSize(File file){
        long fileSize=0;
        if(file.exists())
            fileSize=file.length();
        return fileSize;
    }

    public static long getAvailableSDCardSize(File path) {
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long remainingBlocks = stat.getFreeBlocksLong();
        return remainingBlocks * blockSize;
    }

    public static long getTotalSDCardSize(File path) {
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize;
    }

    public static String formatSize(long size) {
        String suffix = null;
        if (size >= 1024) {
            suffix = " KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = " MB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }
    public static boolean deleteFile(Context context){
        String filePath= getFilePath(context);
        File file = new File(filePath);
        return file.delete();
    }

    public static String getDirectory(Context context, String STORAGE_OPTION) {
        if (context == null) return null;
        Log.d(TAG, "getDirectory.. STORAGE_OPTION=" + STORAGE_OPTION + " Context=" + context);
        String directory;
        switch (STORAGE_OPTION) {
            case INTERNAL_SDCARD:
                directory = getInternalSDCardDirectory(context);
                break;
            case EXTERNAL_SDCARD:
                directory = getExternalSDCardDirectory(context);
                break;
            case INTERNAL_SDCARD_PREFERRED:
                directory = getInternalSDCardDirectory(context);
                break;
            case EXTERNAL_SDCARD_PREFERRED:
                directory = getExternalSDCardDirectory(context);
                if (directory == null)
                    directory = getInternalSDCardDirectory(context);
                break;
            case NONE:
                directory = null;
                break;
            default:
                directory = null;
        }
        return directory;
    }
}
