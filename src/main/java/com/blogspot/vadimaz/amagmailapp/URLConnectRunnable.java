package com.blogspot.vadimaz.amagmailapp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class URLConnectRunnable implements Runnable {
    private URL url;
    public URLConnectRunnable(URL url) {
        this.url = url;
    }

    @Override
    public void run() {
        /*try {
            URLConnection connection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder page = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                page.append(line);
            }
            reader.close();
            System.out.println(page);
        } catch (IOException e) {
            System.out.println("Can't connect to " + url);
        }*/

        try {
            URLConnection connection = url.openConnection();
            connection.connect();
        } catch (IOException e) {
            System.out.println("Can't connect to " + url);
        }
    }
}
