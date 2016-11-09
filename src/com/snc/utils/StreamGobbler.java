package com.snc.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread {
    private InputStream inputStream;
    private String streamType;
    private String code;
    private StringBuilder buf;
    private volatile boolean isStopped = false;

    /**
     * Constructor.
     * 
     * @param inputStream
     * the InputStream to be consumed
     * @param streamType
     * the stream type (should be OUTPUT or ERROR)
     * @param displayStreamOutput
     * whether or not to display the output of the stream being
     * consumed
     */
    public StreamGobbler(final InputStream inputStream, final String streamType, final String code) {
        this.inputStream = inputStream;
        this.streamType = streamType;
        this.code = code;
        this.buf = new StringBuilder();
        this.isStopped = false;
    }

    /**
     * Consumes the output from the input stream and displays the lines consumed
     * if configured to do so.
     */
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, code));

            String str = null;
            while ((str = in.readLine()) != null) {
                if (str.trim().length() == 0) {
                    continue;
                }
                this.buf.append(str).append("\n");
            }
        } catch (IOException ex) {
            System.out.println("[StreamGobbler.run] Failed to successfully consume and display the input stream of type " + streamType
                    + "." + ex);
        } finally {
            this.isStopped = true;
            synchronized (this) {
                notify();
            }
        }
    }

    public String getContent() {
        if (!this.isStopped) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ignore) {
                }
            }
        }
        return this.buf.toString();
    }
}
