/**
 * Copyright (c) LiveV Technologies(China),Inc.
 *
 * @Package: com.snc.utils
 *
 * @FileName: ExecuteCommandUtils.java
 *
 * @Description: TODO(用一句话描述该文件做什么)
 *
 * @author: LC
 *
 * @date 2016年8月30日-下午3:16:28
 *
 * @version 1.0.0
 */
package com.snc.utils;

import java.util.Map;

import com.snc.service.impl.LocalCommandExecutorServiceImpl;
import com.snc.vo.ExecuteResultVo;
import com.snc.vo.ExecuteResultVo.Status;

/**
 * @ClassName: ExecuteCommandUtils
 *
 * @Description: TODO(这里用一句话描述这个类的作用)
 */
public class ExecuteCommandUtils {

    /**
     * 
     * @Title: executeCommand
     * @Description: 执行命令行
     * @param currentExecDir
     * @param command
     * @param timeout
     * @param specificUser
     * @param paramMap
     * @return 参数
     * @return ExecuteResultVo 返回类型
     * @throws
     */
    public static ExecuteResultVo executeCommand(String currentExecDir, String command, Long timeout,
            boolean specificUser, Map<String, String> paramMap) {

        if ("bash".equals(command)) {
            return new ExecuteResultVo(Status.SUCCESS, "");
        } else if ("os.name".equals(command)) {
            return new ExecuteResultVo(Status.SUCCESS, System.getProperty("os.name"));
        } else {
            if (specificUser) {
                return new LocalCommandExecutorServiceImpl().executeCommandByUser(currentExecDir, command, timeout,
                        paramMap);
            }

            return new LocalCommandExecutorServiceImpl().executeCommand(currentExecDir, command, timeout);
        }
    }
}
