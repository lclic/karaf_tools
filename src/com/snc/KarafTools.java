/**
 * Copyright (c) LiveV Technologies(China),Inc.
 *
 * @Package: karaf_tools
 *
 * @FileName: Main.java
 *
 * @Description: TODO(用一句话描述该文件做什么)
 *
 * @author: LC
 *
 * @date 2016年8月23日-下午2:59:27
 *
 * @version 1.0.0
 */
package com.snc;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Properties;

import com.snc.utils.ExecuteCommandUtils;
import com.snc.utils.FileUtils;
import com.snc.vo.ExecuteResultVo;
import com.snc.vo.InstancePropertiesModel;

/**
 * @ClassName: Main
 *
 * @Description: TODO(这里用一句话描述这个类的作用)
 */
public class KarafTools {
    private static String KARAF_DIR_STR = null;
    private static String START_COMMAND_STR = "start";
    private static String STOP_COMMAND_STR = "stop";
    private static String UPDATE_COMMAND_STR = "update";
    private static String RESTART_COMMAND_STR = "restart";

    private static String DATA_DIR_STR = "data";
    private static String UPDATE_DIR_STR = "update";
    private static String BACKUP_DIR_STR = "backup";
    private static String DEPLOY_DIR_STR = "deploy";
    private static String debugPrintFlag = "debug"; // 用于控制debug日志的输入，0为默认不输出;1为输出debug日志
    private static long timeout = 30000L;
    private static boolean operateSuccess = false;

    private static String STOP_KARAF_COMMAND_FOR_WINDOWS = "." + File.separatorChar + "bin" + File.separatorChar
            + "stop.bat";
    private static String STOP_KARAF_COMMAND_FOR_LINUX = "sh ." + File.separatorChar + "bin" + File.separatorChar
            + "stop";
    private static String START_KARAF_COMMAND_FOR_WINDOWS = "." + File.separatorChar + "bin" + File.separatorChar
            + "start.bat";
    private static String START_KARAF_COMMAND_FOR_LINUX = "sh ." + File.separatorChar + "bin" + File.separatorChar
            + "start";

    public static void main(String[] args) {
        // 校验参数
        if (!verifyArgs(args)) {
            return;
        }

        KARAF_DIR_STR = args[0];
        String commandStr = args[1];
        debugPrintFlag = args[2];
        KarafTools.outPrint("==============[KarafTools.main] karaf tools start==============", false);

        // 获取instanceProperties model
        InstancePropertiesModel instancePropertiesModel = getInstanceProperties(KARAF_DIR_STR);

        if (START_COMMAND_STR.equals(commandStr) || RESTART_COMMAND_STR.equals(commandStr)) {
            // 停止karaf进程
            stopKarafByPid(KARAF_DIR_STR, instancePropertiesModel.getPid());

            // 因为涉及多个操作，所以重新设置操作成功标志为false,只有最有一个步骤成功了才是全过程成功
            operateSuccess = false;
            // 重新karaf程序并验证，如果失败则回退
            startKaraf(KARAF_DIR_STR);
        } else if (UPDATE_COMMAND_STR.equals(commandStr)) {
            UPDATE_DIR_STR = KARAF_DIR_STR + File.separatorChar + UPDATE_DIR_STR;
            BACKUP_DIR_STR = KARAF_DIR_STR + File.separatorChar + BACKUP_DIR_STR;
            DEPLOY_DIR_STR = KARAF_DIR_STR + File.separatorChar + DEPLOY_DIR_STR;
            DATA_DIR_STR = KARAF_DIR_STR + File.separatorChar + DATA_DIR_STR;

            // 获取待升级程序包，并校验
            if (!verifyUpdateFiles()) {
                // 清空update目录
                FileUtils.delete(new File(UPDATE_DIR_STR));
                return;
            }

            // 停止karaf进程
            stopKarafByPid(KARAF_DIR_STR, instancePropertiesModel.getPid());
            // 因为涉及多个操作，所以重新设置操作成功标志为false,只有最有一个步骤成功了才是全过程成功
            operateSuccess = false;
            // 删除data目录
            FileUtils.delete(new File(DATA_DIR_STR));

            // 把待更新文件解压到deploy目录中
            copyUpdateFilesToDeployDir();

            // 清空update目录
            FileUtils.delete(new File(UPDATE_DIR_STR));

            // 重新karaf程序并验证，如果失败则回退
            startKaraf(KARAF_DIR_STR);
        } else if (STOP_COMMAND_STR.equals(commandStr)) {
            // 停止karaf进程
            stopKarafByPid(KARAF_DIR_STR, instancePropertiesModel.getPid());
        }

        if (operateSuccess) {
            KarafTools.outPrint("OPERATE:SUCCESS", true);
        } else {
            KarafTools.outPrint("OPERATE:FAIL", true);
        }

    }

    /**
     * 
     * @Title: verifyArgs
     * @Description: 校验参数
     * @param args
     * @return 参数
     * @return boolean 返回类型
     * @throws
     */
    public static boolean verifyArgs(String[] args) {
        if (args == null || args.length <= 0) {
            KarafTools
                    .outPrint(
                            "[KarafTools.verifyArgs] require params,please input params,eg:java -jar karaf_tools.jar ${karaf_dir} ${start|stop|update|restart} ${debug_flag}",
                            true);
            return false;
        }

        if (args.length != 3) {
            KarafTools
                    .outPrint(
                            "[KarafTools.verifyArgs] params length must be three,eg:java -jar karaf_tools.jar ${karaf_dir} ${start|stop|update|restart} ${debug_flag}",
                            true);
            return false;
        }

        return true;
    }

    /**
     * 
     * @Title: getInstanceProperties
     * @Description: 获取karaf目录下的instance目录下的instance.properties文件
     * 需要注意除了该文件的pid每次都能被正确更新外，其他的属性并不准确，不能作为决定性参考
     * @param karafDirPath
     * @return 参数
     * @return InstancePropertiesModel 返回类型
     * @throws
     */
    public static InstancePropertiesModel getInstanceProperties(String karafDirPath) {
        KarafTools.outPrint("[KarafTools.getInstanceProperties] ################start#################", false);
        try {
            if (karafDirPath == null || karafDirPath.length() <= 0) {
                KarafTools.outPrint("[KarafTools.getInstanceProperties]  karaf dir is null,please check it again!",
                        false);
                return null;
            }

            File karafDir = new File(karafDirPath);
            if (karafDir == null || (!karafDir.exists())) {
                KarafTools.outPrint("[KarafTools.getInstanceProperties]  karaf dir:[" + karafDirPath
                        + "] is not exist!", false);
                return null;
            }

            // 查找instance目录，并找出instance.properties文件
            File[] instanceDirFiles = karafDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.isDirectory() && pathname.getName().equals("instances")) {
                        return true;
                    }
                    return false;
                }
            });

            if (instanceDirFiles == null || instanceDirFiles.length <= 0) {
                return null;
            }

            // 查找出instance目录下的instance.properties文件
            File instanceDir = instanceDirFiles[0];

            File[] propertiesFiles = instanceDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.isFile() && pathname.getName().equals("instance.properties")) {
                        return true;
                    }
                    return false;
                }
            });

            if (propertiesFiles == null || propertiesFiles.length <= 0) {
                return null;
            }
            File propertiesFile = propertiesFiles[0];

            // 生成文件输入流
            FileInputStream inputStream = null;
            inputStream = new FileInputStream(propertiesFile);

            // 生成properties对象
            Properties properties = new Properties();
            properties.load(inputStream);

            InstancePropertiesModel model = InstancePropertiesModel.transformProperties(properties);

            KarafTools.outPrint("[KarafTools.getInstanceProperties] count:" + model.getCount(), false);
            KarafTools.outPrint("[KarafTools.getInstanceProperties] loc:" + model.getLoc(), false);
            KarafTools.outPrint("[KarafTools.getInstanceProperties] name:" + model.getName(), false);
            KarafTools.outPrint("[KarafTools.getInstanceProperties] pid:" + model.getPid(), false);
            KarafTools.outPrint("[KarafTools.getInstanceProperties] root:" + model.getRoot(), false);
            KarafTools.outPrint("[KarafTools.getInstanceProperties] ################end#################", false);
            return model;
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * 
     * @Title: verifyUpdateFiles
     * @Description: 校验升级文件（要求升级文件必须为zip文件，并且至少包含有一个jar包文件，其他非jar包文件为非法文件，不做替换任何操作）
     * @return 参数
     * @return boolean 返回类型
     * @throws
     */
    public static boolean verifyUpdateFiles() {
        KarafTools.outPrint("[KarafTools.verifyUpdateFiles] ################start#################", false);
        // 解压程序包

        File updateFilesDir = new File(UPDATE_DIR_STR);

        if (!updateFilesDir.isDirectory()) {
            KarafTools.outPrint("[KarafTools.verifyUpdateFiles] " + UPDATE_DIR_STR + " is not a directory", true);
            return false;
        }

        // 首先删除非zip的文件
        KarafTools.outPrint(
                "[KarafTools.verifyUpdateFiles] scan files in update directory which name is not end with zip", false);
        File[] notZipFiles = updateFilesDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !(file.getName().toLowerCase().endsWith(".zip"));
            }
        });

        if (notZipFiles != null && notZipFiles.length > 0) {
            for (File file : notZipFiles) {
                String fileName = file.getAbsolutePath();
                file.delete();
                KarafTools.outPrint("[KarafTools.verifyUpdateFiles]  delete encorrect file,which name is " + fileName,
                        false);
            }
        }

        File[] updateFiles = updateFilesDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.getName().toLowerCase().endsWith(".zip");
            }
        });

        if (updateFiles == null || updateFiles.length <= 0) {
            return false;
        }

        for (File file : updateFiles) {
            KarafTools.outPrint("[KarafTools.verifyUpdateFiles] get a zip file,the path is " + file.getPath(), false);
            // 解压升级包
            File updateFileDir = null;
            try {
                updateFileDir = FileUtils.expandZipFile(updateFilesDir, file);
                // 验证该zip是否符合要求
                if (updateFileDir == null || !updateFileDir.isDirectory()) {
                    KarafTools.outPrint("[KarafTools.verifyUpdateFiles] this zip file:" + file.getPath(), false);
                    // 删除该目录和该文件
                    file.delete();
                    updateFileDir.delete();
                    continue;
                }

                KarafTools.outPrint("[KarafTools.verifyUpdateFiles] success to expand this zip file:" + file.getPath(),
                        false);

                // 查找目录下是否存在jar包
                File[] jarFiles = updateFileDir.listFiles(new FileFilter() {
                    public boolean accept(File file) {
                        return file.getName().toLowerCase().endsWith(".jar");
                    }
                });

                if (jarFiles == null || jarFiles.length <= 0) {
                    KarafTools
                            .outPrint(
                                    "[KarafTools.verifyUpdateFiles] this zip file must be have a jar file contain,but this one have none",
                                    false);
                    return false;
                }
            } catch (Exception e) {
                KarafTools.outPrint("[KarafTools.verifyUpdateFiles] scanUpdateDir has error:" + e.getMessage(), false);
            } finally {
                updateFileDir.delete();
            }
        }
        KarafTools.outPrint("[KarafTools.verifyUpdateFiles] ################end#################", false);

        return true;
    }

    /**
     * 
     * @Title: copyUpdateFilesToDeployDir
     * @Description: 把升级文件解压后得到jar文件，并将其移动至deploy目录
     * @return 参数
     * @return boolean 返回类型
     * @throws
     */
    public static boolean copyUpdateFilesToDeployDir() {
        // 解压程序包
        KarafTools.outPrint("[KarafTools.copyUpdateFilesToDeployDir] ################start#################", false);
        File updateFilesDir = new File(UPDATE_DIR_STR);
        File deployFilesDir = new File(DEPLOY_DIR_STR);

        if (!deployFilesDir.exists()) {
            deployFilesDir.mkdir();
        }

        if (!updateFilesDir.isDirectory()) {
            return false;
        }

        File[] updateFiles = updateFilesDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.getName().toLowerCase().endsWith(".zip");
            }
        });

        if (updateFiles == null || updateFiles.length <= 0) {
            return false;
        }

        for (File file : updateFiles) {
            KarafTools.outPrint("[KarafTools.copyUpdateFilesToDeployDir] get a zip file,the path is " + file.getPath(),
                    false);
            // 解压升级包
            File updateFileDir = null;
            try {
                updateFileDir = FileUtils.expandZipFile(updateFilesDir, file);

                File[] notJarFiles = updateFileDir.listFiles(new FileFilter() {
                    public boolean accept(File file) {
                        return !file.getName().toLowerCase().endsWith(".jar");
                    }
                });

                if (notJarFiles != null && notJarFiles.length > 0) {
                    for (File notJarFile : notJarFiles) {
                        String fileName = notJarFile.getAbsolutePath();
                        notJarFile.delete();
                        KarafTools.outPrint(
                                "[KarafTools.copyUpdateFilesToDeployDir] delete encorrect jar file,which name is "
                                        + fileName, false);
                    }
                }

                FileUtils.copyDirectiory(updateFileDir, deployFilesDir);

                // 验证该zip是否符合要求
                if (updateFileDir == null || !updateFileDir.isDirectory()) {
                    KarafTools.outPrint("[KarafTools.copyUpdateFilesToDeployDir] this zip file:" + file.getPath(),
                            false);
                    // 删除该目录和该文件
                    file.delete();
                    updateFileDir.delete();
                    continue;
                }

                KarafTools.outPrint(
                        "[KarafTools.copyUpdateFilesToDeployDir] success to expand this zip file:" + file.getPath(),
                        false);
            } catch (Exception e) {
                KarafTools.outPrint(
                        "[KarafTools.copyUpdateFilesToDeployDir] scanUpdateDir has error:" + e.getMessage(), true);
            } finally {
            }
        }
        KarafTools.outPrint("[KarafTools.copyUpdateFilesToDeployDir] ################end#################", false);

        return true;
    }

    /**
     * 
     * @Title: stopKarafByPid
     * @Description: 停止karaf
     * @param currentExecDir
     * @param pid
     * @return 参数
     * @return boolean 返回类型
     * @throws
     */
    public static boolean stopKarafByPid(String currentExecDir, String pid) {
        KarafTools.outPrint("[KarafTools.stopKarafByPid] ################start#################", false);
        ExecuteResultVo result = null;
        // 识别操作系统类型
        if (ifWindowsOs()) {
            KarafTools.outPrint("[KarafTools.stopKarafByPid] start to run command for windows:"
                    + STOP_KARAF_COMMAND_FOR_WINDOWS, false);

            // 首先通过常规的bin/stop来进行停止
            ExecuteCommandUtils.executeCommand(currentExecDir, STOP_KARAF_COMMAND_FOR_WINDOWS, timeout, false, null);

            /**
             * 
             * windows下有三种情形:
             * 1、调用bin/start.bat运行，程序进入后台运行，但是还是有一个cmd控制台出来
             * 此时，其commandline输出如下
             * C:\Users\LC>wmic process get name,commandline,processid|findstr 7632
             * C:\Windows\system32\cmd.exe /K "D:\apache-karaf-2.3.11\bin\..\bin\karaf.bat" server
             * 
             * 
             * 2、调用bin/karaf.bat运行，程序直接以前台形式运行，日志都在此控制台输出
             * 此时，其commandline输出会包含有"org.apache.karaf.main.Main"
             * 
             * 
             * 3、以服务的形式集成
             * 此时，其commandline输出会包含有?
             * 
             * 停止和运行的命令为：
             * net start 服务名
             * net stop 服务名
             * 服务名不分大小写
             * net start|findstr /i "karaf"
             * 
             * windows 查找进程信息和kill进程相关说明:
             * 根据pid查找windos进程相关信息 wmic process get name,executablepath,CommandLine,processid|findstr 6080
             * 强行终止windows进程 taskkill/F /pid 9999
             * 首先ps一下pid，查看下是否为karaf进程，不能杀错进程
             * 
             */

            // 查找karaf服务是否还存在，如果还存在，尝试以服务的形式进行停止
            String findServiceCommand = "net start|findstr /i \"karaf\"";
            result = null;
            result = ExecuteCommandUtils.executeCommand(currentExecDir, findServiceCommand, timeout, false, null);

            KarafTools.outPrint("[KarafTools.stopKarafByPid] find karaf service,command is:" + findServiceCommand
                    + " result:" + result.getExecuteOut(), false);

            if (result.getExecuteOut().contains("karaf")) {

            }

            // 查找是否还存在karaf进程，上面两种形式都无法停止karaf，则直接杀karaf进程
            String findPidCommand = "wmic process get name,executablepath,CommandLine,processid|findstr " + pid;
            result = ExecuteCommandUtils.executeCommand(currentExecDir, findPidCommand, timeout, false, null);
            KarafTools.outPrint("[KarafTools.stopKarafByPid] find pid result:" + result.getExecuteOut(), false);

            if (result.getExecuteOut().contains("org.apache.karaf.main.Main")
                    || result.getExecuteOut().contains("karaf.bat")) {
                KarafTools.outPrint("[KarafTools.stopKarafByPid] find correct karaf pid,start to kill this pid:" + pid,
                        false);
                STOP_KARAF_COMMAND_FOR_LINUX = "taskkill/F /pid " + pid;
                KarafTools.outPrint("[KarafTools.stopKarafByPid] start to run command:" + STOP_KARAF_COMMAND_FOR_LINUX,
                        false);
                result = ExecuteCommandUtils.executeCommand(currentExecDir, STOP_KARAF_COMMAND_FOR_LINUX, timeout,
                        false, null);
            } else {
                KarafTools.outPrint("[KarafTools.stopKarafByPid] karaf stop already,cannot find correct karaf pid:"
                        + pid, false);
            }
        } else {
            KarafTools.outPrint("[KarafTools.stopKarafByPid] start to run command for linux:"
                    + STOP_KARAF_COMMAND_FOR_WINDOWS, false);

            // 调用常规的命令停止karaf
            ExecuteCommandUtils.executeCommand(currentExecDir, STOP_KARAF_COMMAND_FOR_LINUX, timeout, false, null);

            // 首先ps一下pid，查看下是否为karaf进程，不能杀错进程
            String findPidCommand = "ps -ef|grep " + pid;
            KarafTools.outPrint("[KarafTools.stopKarafByPid] find pid:" + pid + " info", false);
            result = ExecuteCommandUtils.executeCommand(currentExecDir, findPidCommand, timeout, false, null);
            KarafTools.outPrint("[KarafTools.stopKarafByPid] find pid result:" + result.getExecuteOut(), false);

            if (result.getExecuteOut().contains("org.apache.karaf.main.Main")) {
                KarafTools.outPrint("[KarafTools.stopKarafByPid] find correct karaf pid,start to kill this pid:" + pid,
                        false);
                STOP_KARAF_COMMAND_FOR_LINUX = "kill -9 " + pid;
                KarafTools.outPrint("[KarafTools.stopKarafByPid] start to run command:" + STOP_KARAF_COMMAND_FOR_LINUX,
                        false);
                result = ExecuteCommandUtils.executeCommand(currentExecDir, STOP_KARAF_COMMAND_FOR_LINUX, timeout,
                        false, null);
            } else {
                KarafTools.outPrint("[KarafTools.stopKarafByPid] karaf stop already,cannot find correct karaf pid:"
                        + pid, false);
            }
        }

        // 设置操作成功标志
        operateSuccess = true;

        KarafTools.outPrint("[KarafTools.stopKarafByPid] ################end#################", false);
        return true;
    }

    /**
     * 
     * @Title: startKaraf
     * @Description: 调用karaf自身的启动脚本启动karaf
     * @return 参数
     * @return boolean 返回类型
     * @throws
     */
    public static boolean startKaraf(String currentExecDir) {
        KarafTools.outPrint("[KarafTools.startKaraf] ################start#################", false);
        ExecuteResultVo result = null;
        if (ifWindowsOs()) {

            /**
             * 
             * windows下有三种启动karaf的形式：
             * 
             * windows下有三种情形:
             * 1、调用bin/start.bat运行
             * 
             * 2、调用bin/karaf.bat运行
             * 
             * 
             * 3、以服务的形式运行
             * net start 服务名
             * net stop 服务名
             * 服务名不分大小写
             * net start|findstr /i "karaf"
             * 
             */

            KarafTools.outPrint("[KarafTools.startKaraf] start to run command:" + START_KARAF_COMMAND_FOR_WINDOWS,
                    false);
            result = ExecuteCommandUtils.executeCommand(currentExecDir, START_KARAF_COMMAND_FOR_WINDOWS, timeout,
                    false, null);
        } else {
            KarafTools.outPrint("[KarafTools.startKaraf] start to run command:" + START_KARAF_COMMAND_FOR_LINUX, false);
            result = ExecuteCommandUtils.executeCommand(currentExecDir, START_KARAF_COMMAND_FOR_LINUX, timeout, false,
                    null);
        }

        // 设置操作成功标志
        operateSuccess = true;

        KarafTools.outPrint("[KarafTools.startKaraf] ################end#################", false);
        return true;
    }

    /**
     * 
     * @Title: ifWindowsOs
     * @Description: 判断操作系统是否为windows
     * @return 参数
     * @return boolean 返回类型
     * @throws
     */
    private static boolean ifWindowsOs() {
        String os = System.getProperty("os.name");
        KarafTools.outPrint("[KarafTools.ifWindowsOs] this os system is: " + os, false);
        if (os.toLowerCase().contains("win")) {
            return true;
        }

        return false;
    }

    /**
     * 
     * @Title: outPrint
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param message
     * @param forceOutPrint 强制输出
     * @return void 返回类型
     * @throws
     */
    public static void outPrint(String message, boolean forceOutPrint) {
        if ("debug".equals(debugPrintFlag) || forceOutPrint) {
            System.out.println(message);
        }
    }
}
