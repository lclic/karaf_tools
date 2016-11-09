package com.snc.service.impl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.nemo.javaexpect.shell.CommandResult;
import com.nemo.javaexpect.shell.Shell;
import com.nemo.javaexpect.shell.driver.DefaultShellDriver;
import com.nemo.javaexpect.shell.driver.SshDriver;
import com.snc.Constants;
import com.snc.KarafTools;
import com.snc.service.LocalCommandExecutorService;
import com.snc.utils.StreamGobbler;
import com.snc.vo.ExecuteResultVo;
import com.snc.vo.ExecuteResultVo.Status;

public class LocalCommandExecutorServiceImpl implements LocalCommandExecutorService {

    static ExecutorService pool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());

    @Override
    public ExecuteResultVo executeCommand(String currentExecDir, String command, Long timeout) {
        Process process = null;
        InputStream pIn = null;
        InputStream pErr = null;
        StreamGobbler outputGobbler = null;
        StreamGobbler errorGobbler = null;
        Future<Integer> executeFuture = null;

        ProcessBuilder pp = null;
        List<String> cmds = new ArrayList<String>();
        String code = "UTF-8";

        if (System.getProperty("os.name").startsWith("Windows")) {
            cmds.add("cmd.exe");
            cmds.add("/c");
            cmds.add(command);
            code = "GBK";
        } else {
            cmds.add("sh");
            cmds.add("-c");
            cmds.add(command);
        }
        pp = new ProcessBuilder(cmds);

        if (null != currentExecDir && currentExecDir.length() > 0) {
            pp.directory(new File(currentExecDir));
        }

        pp.redirectErrorStream(true);

        try {
            process = pp.start();

            final Process p = process;
            // close process's output stream.
            p.getOutputStream().close();

            pIn = process.getInputStream();
            outputGobbler = new StreamGobbler(pIn, "OUTPUT", code);
            outputGobbler.start();

            pErr = process.getErrorStream();
            errorGobbler = new StreamGobbler(pErr, "ERROR", code);
            errorGobbler.start();

            // create a Callable for the command's Process which can be called by an Executor
            Callable<Integer> call = new Callable<Integer>() {
                public Integer call() throws Exception {
                    p.waitFor();
                    return p.exitValue();
                }
            };

            // submit the command's call and get the result from a
            executeFuture = pool.submit(call);
            int exitCode = executeFuture.get(timeout, TimeUnit.MILLISECONDS);
            String output = outputGobbler.getContent();
            KarafTools.outPrint("[LocalCommandExecutorServiceImpl.executeCommand] ExecuteResultVo: [ExitCode: "
                    + exitCode + ", Output: " + output + "]", false);
            return new ExecuteResultVo(exitCode == 0 ? Status.SUCCESS : Status.FAILURE, output);
        } catch (IOException ex) {
            String errorMessage = "The command [" + command + "] execute failed.";

            KarafTools.outPrint("[LocalCommandExecutorServiceImpl.executeCommand] error:" + errorMessage, true);

            return new ExecuteResultVo(Status.FAILURE, errorMessage);
        } catch (TimeoutException ex) {
            String errorMessage = "The command [" + command + "] timed out.";

            KarafTools.outPrint("[LocalCommandExecutorServiceImpl.executeCommand] error:" + errorMessage, true);

            return new ExecuteResultVo(Status.FAILURE, errorMessage);
        } catch (ExecutionException ex) {
            String errorMessage = "The command [" + command + "] did not complete due to an execution error.";

            KarafTools.outPrint("[LocalCommandExecutorServiceImpl.executeCommand] error:" + errorMessage, true);

            return new ExecuteResultVo(Status.FAILURE, errorMessage);
        } catch (InterruptedException ex) {
            String errorMessage = "The command [" + command + "] did not complete due to an interrupted error.";

            KarafTools.outPrint("[LocalCommandExecutorServiceImpl.executeCommand] error:" + errorMessage, true);

            return new ExecuteResultVo(Status.FAILURE, errorMessage);
        } finally {
            if (executeFuture != null) {
                try {
                    executeFuture.cancel(true);
                } catch (Exception ignore) {
                }
            }
            if (pIn != null) {
                this.closeQuietly(pIn);
                if (outputGobbler != null && !outputGobbler.isInterrupted()) {
                    outputGobbler.interrupt();
                }
            }
            if (pErr != null) {
                this.closeQuietly(pErr);
                if (errorGobbler != null && !errorGobbler.isInterrupted()) {
                    errorGobbler.interrupt();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Override
    public ExecuteResultVo executeCommandByUser(String currentExecDir, String command, Long timeout,
            Map<String, String> paramMap) {

        ExecuteResultVo ExecuteResultVo = new ExecuteResultVo();

        if (paramMap == null) {
            return new ExecuteResultVo(Status.FAILURE, "please assign username and password");
        }
        String loginUserName = paramMap.get(Constants.LOGIN_USER_NAME);
        String loginUserPw = paramMap.get(Constants.LOGIN_USER_PW);
        String rootPw = paramMap.get(Constants.ROOT_PW);

        String expectStr = "((]\\$)|]#)";
        // 需要切换用户来执行命令
        Shell shell = null;
        int timeOut = 10000;
        try { // IP改成本机 zgh
            DefaultShellDriver driver = new SshDriver("127.0.0.1", loginUserName, loginUserPw, expectStr);
            driver.setSkipVT100Filter(true);// 过滤特殊字符 zgh
            shell = driver.open();
            // 需要切换root
            if (rootPw != null) {
                shell.send("su - root");
                // shell.expect(":", timeOut);//注释掉这行,发现加了失败 zgh
                Thread.sleep(1000 * 1);
                shell.send(rootPw);
                CommandResult suCr = shell.expect(expectStr, timeOut);
                KarafTools.outPrint(
                        "[LocalCommandExecutorServiceImpl.executeCommandByUser] su user result:"
                                + suCr.getCommandResult(), false);
                shell.send(command);
                CommandResult cr = shell.expect(expectStr, timeOut);
                ExecuteResultVo.setStatus(Status.SUCCESS);
                ExecuteResultVo.setExecuteOut(cr.getCommandResult());
                KarafTools.outPrint(
                        ("[LocalCommandExecutorServiceImpl.executeCommandByUser] su user and execute cmd result:" + cr
                                .getCommandResult()), false);
            } else {
                shell.send(command);
                CommandResult cr = shell.expect(expectStr, timeOut);
                ExecuteResultVo.setStatus(Status.SUCCESS);
                ExecuteResultVo.setExecuteOut(cr.getCommandResult());
            }
        } catch (Exception e) {
            ExecuteResultVo.setStatus(Status.FAILURE);
            ExecuteResultVo.setExecuteOut(e.getMessage());
            KarafTools.outPrint(e.getMessage(), true);
        } finally {
            if (shell != null) {
                shell.close();
            }
        }
        return ExecuteResultVo;
    }

    private void closeQuietly(Closeable c) {
        try {
            if (c != null)
                c.close();
        } catch (IOException e) {
        }
    }

}
