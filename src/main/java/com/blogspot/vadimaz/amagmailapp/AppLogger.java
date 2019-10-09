package com.blogspot.vadimaz.amagmailapp;

import java.util.Date;

public class AppLogger {
    public static synchronized void info(String s) {
        System.out.println(getDate() + " INFO " + s);
    }

    public static synchronized void error(String s) {
        System.out.println(getDate() + " ERROR " + s);
    }
    private static String getDate() {
        return new Date().toString();
    }
}
