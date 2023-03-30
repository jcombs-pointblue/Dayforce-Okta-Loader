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

    public Logger(int logLevel, String logPath) throws IOException {
        this.logLevel = logLevel;
        this.log = new FileWriter(logPath);
        log.write("New Execution at: "+ new Date().toString()+"\n");
        log.flush();

    }

    public static int DEBUG = 3;
    public static int NORMAL = 1;

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

    public void trace(int i, String string) {
        System.out.println(string);
    }

    public void trace(int i, int i1, String string) {
        System.out.println(string);

    }

    public void trace(String string, int i) {
        System.out.println(string);
    }

    public int getLevel() {
        return 3;
    }

}
