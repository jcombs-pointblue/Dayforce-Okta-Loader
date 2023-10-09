/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stevens.okta;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 *
 * @author jcombs
 */
public class Logger {

    private FileWriter log;
    int logLevel;

    /**
     *
     * @param logLevel
     * @param logPath
     * @throws IOException
     */
    public Logger(int logLevel, String logPath) throws IOException {
        this.logLevel = logLevel;
        this.log = new FileWriter(logPath);
        log.write("New Execution at: "+ new Date().toString()+"\n");
        log.flush();

    }

    /**
     *
     */
    public static int DEBUG = 3;

    /**
     *
     */
    public static int NORMAL = 1;

    /**
     *
     * @param level
     * @param logMsg
     */
    public void Log(int level, String logMsg) {
        
        System.out.println(logMsg);
        try {
            if (level <= logLevel) {
                log.write(logMsg +"\n");
                
                log.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    /**
     *
     * @param i
     * @param string
     */
    public void trace(int i, String string) {
        System.out.println(string);
    }

    /**
     *
     * @param i
     * @param i1
     * @param string
     */
    public void trace(int i, int i1, String string) {
        System.out.println(string);

    }

    /**
     *
     * @param string
     * @param i
     */
    public void trace(String string, int i) {
        System.out.println(string);
    }

    /**
     *
     * @return
     */
    public int getLevel() {
        return 3;
    }

}
