package com.snc.vo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ExecuteResultVo {

	// 执行状态(0成功,1失败)
	private Status status;

	// 执行结果
	private String executeOut;

	private File file;
	private Map resultMap = new HashMap();

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public enum Status {
		SUCCESS(1), FAILURE(0);

		private int value;

		private Status(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}

	public ExecuteResultVo() {
	}

	public ExecuteResultVo(Status status, String executeOut) {
		super();
		this.status = status;
		this.executeOut = executeOut;
	}

	public ExecuteResultVo(Status status, String executeOut, File file) {
		this(status, executeOut);
		this.file = file;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getExecuteOut() {
		return executeOut;
	}

	public void setExecuteOut(String executeOut) {
		this.executeOut = executeOut;
	}

	public Map getResultMap() {
		return resultMap;
	}

	public void setResultMap(String key, Object value) {
		this.resultMap.put(key, value);
	}

}
