package com.snc.service;

import java.util.Map;

import com.snc.vo.ExecuteResultVo;

public interface LocalCommandExecutorService {

    /**
     * 执行命令
     * 
     * @param currentExecDir
     * @param command
     * @param timeout
     * @param controller
     * @return
     */
    public ExecuteResultVo executeCommand(String currentExecDir, String command, Long timeout);

    /**
     * 使用指定用户执行命令(需要切换用户)
     * 
     * @param currentExecDir
     * @param command
     * @param timeout
     * @param controller
     * @param paramMap
     * @return
     */
    public ExecuteResultVo executeCommandByUser(String currentExecDir, String command, Long timeout,
            Map<String, String> paramMap);

}
