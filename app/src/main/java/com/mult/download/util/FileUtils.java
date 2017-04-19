package com.mult.download.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by SnowDragon on 2017/4/7.
 */
public class FileUtils {

    /**
     * 获取本地文件大小
     *
     * @return 本地文件的大小 or 不存在返回-1
     */
    public static long getLocalFileSize(String localFileName) {
        long size = -1l;
        File localFile = new File(localFileName);
        if (localFile.exists()) {
            size = localFile.length();
        }
        // Log.log("本地文件大小" + size);
        return size <= 0 ? 0 : size;
    }

    /**
     * 获取指定文件大小
     *
     * @param
     * @return
     * @throws Exception
     */
    public static int getFileSize(File file) {
        int size = 0;

        try {
            if (file.exists()) {
                FileInputStream fis = null;
                fis = new FileInputStream(file);
                size = fis.available();

                if (fis != null) fis.close();
            } else {
                file.createNewFile();
                Log.e("获取文件大小", "文件不存在!");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return size;
    }
}
