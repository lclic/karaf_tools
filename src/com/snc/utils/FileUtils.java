package com.snc.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @ClassName: FileUtils
 *
 * @Description: File操作工具类
 */
public class FileUtils {

    public static List<String> readLines(File file, boolean ignoreComments) throws IOException {
        if (!file.exists() || !file.isFile()) {
            return new ArrayList();
        }

        List<String> lines = new ArrayList();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (ignoreComments && !line.startsWith("#") && !lines.contains(line)) {
                    lines.add(line);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return lines;
    }

    public static void writeLines(Collection<String> lines, File file) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Delete a file or recursively delete a folder.
     *
     * @param fileOrFolder
     * @return true, if successful
     */
    public static boolean delete(File fileOrFolder) {
        boolean success = false;
        if (fileOrFolder.isDirectory()) {
            File[] files = fileOrFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        success |= delete(file);
                    } else {
                        success |= file.delete();
                    }
                }
            }
        }
        success |= fileOrFolder.delete();

        return success;
    }

    /**
     * 
     * @Title: copyFile
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param sourceFile
     * @param targetFile
     * @throws IOException 参数
     * @return void 返回类型
     * @throws
     */
    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        try {
            if (targetFile.exists()) {
                targetFile.delete();
            } else {
                targetFile.createNewFile();
            }

            // 新建文件输入流并对它进行缓冲
            inBuff = new BufferedInputStream(new FileInputStream(sourceFile));

            // 新建文件输出流并对它进行缓冲
            outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

            // 缓冲数组
            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            // 刷新此缓冲的输出流
            outBuff.flush();
        } finally {
            // 关闭流
            if (inBuff != null)
                inBuff.close();
            if (outBuff != null)
                outBuff.close();
        }
    }

    /**
     * 
     * @Title: copyDirectiory
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param sourceDir
     * @param targetDir
     * @throws IOException 参数
     * @return void 返回类型
     * @throws
     */
    public static void copyDirectiory(File sourceDir, File targetDir) throws IOException {
        // 获取源文件夹当前下的文件或目录
        File[] file = sourceDir.listFiles();
        for (int i = 0; i < file.length; i++) {
            if (file[i].isFile()) {
                // 源文件
                File sourceFile = file[i];
                // 目标文件
                File targetFile = new File(targetDir, file[i].getName());
                copyFile(sourceFile, targetFile);
            }
            if (file[i].isDirectory()) {
                // 创建目标文件夹
                File d = new File(targetDir, file[i].getName());
                if (!d.exists() || !d.isDirectory()) {
                    d.delete();
                }
                d.mkdir();
                copyDirectiory(file[i], new File(targetDir, file[i].getName()));
            }
        }
    }

    /**
     * 
     * @Title: expandZipFile
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param destDir
     * @param archiveFile
     * @return
     * @throws IOException 参数
     * @return File 返回类型
     * @throws
     */
    public static File expandZipFile(File destDir, File archiveFile) throws IOException {
        try {
            String fileName = archiveFile.getName();
            String pluginName = fileName.substring(0, fileName.length() - 4);
            File updateFileDir = new File(destDir, pluginName);

            System.out.println("[FileUtils.expandZipFile] Expand update archive:" + archiveFile + " in "
                    + updateFileDir);

            // create directory for plugin
            updateFileDir.mkdirs();

            // expand '.zip' file
            UnzipUtils unzip = new UnzipUtils();
            unzip.setSource(archiveFile);
            unzip.setDestination(updateFileDir);
            unzip.extract();
            return updateFileDir;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

}
