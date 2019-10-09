package com.blogspot.vadimaz.amagmailapp;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AppLogger {
    private static final Logger LOGGER = Logger.getLogger(AMAGmailApp.class.getName());
    public static synchronized void info(String s) {
        //StringBuilder sb = new StringBuilder();
        //sb.append(new Date()).append(" ").append(s).append("\n");
        LOGGER.log(Level.INFO, s);
        //System.out.println(sb.toString());
    }

    public static synchronized void error(String s) {
        //StringBuilder sb = new StringBuilder();
        //sb.append(new Date()).append(" ").append(s).append("\n");
        LOGGER.log(Level.WARNING, s);
        //System.out.println(sb.toString());
    }
}
