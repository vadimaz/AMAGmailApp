package com.blogspot.vadimaz.amagmailapp;

import com.google.api.services.gmail.Gmail;

import javax.mail.MessagingException;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class URLConnectRunnable implements Runnable {
    private URL urlAddress;
    private Gmail service;
    private static final String TO = "vzorenko@gmail.com";
    private static final String FROM = "info@eliterestorationteam.com";

    public URLConnectRunnable(URL urlAddress, Gmail service) {
        this.urlAddress = urlAddress;
        this.service = service;
    }

    @Override
    public void run() {

        HttpURLConnection connection = null;
        try {
            connection = getHttpConnection(urlAddress); // getting http or https url connection
        } catch (IOException e) {
            AppLogger.error(String.format("Unable to get connection for URL: %s", urlAddress));
            e.printStackTrace();
        }

        String response = getContent(connection); // reading html from connection

        // print response and send mail with response
        if (connection != null && response != null) {
           //System.out.println(response); // printing the response
            try {
                MailSender.sendMessage(service, GmailService.USER, MailSender.createEmail(TO, FROM, "AmaGmailApp " + connection.getURL(), response));
            } catch (MessagingException | IOException e) {
                AppLogger.error(String.format("Unable to send message 'AmaGmailApp %s' to %s", connection.getURL(), TO));
                e.printStackTrace();
            }
        }
    }

    private HttpURLConnection getHttpConnection(URL url) throws IOException {
        if (url != null) {
            if (url.getProtocol().equalsIgnoreCase("https")) {
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                return getConnectionIfRedirect(connection);

            } else {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                return getConnectionIfRedirect(connection);
            }
        }
        return null;
    }
    private HttpURLConnection getConnectionIfRedirect(HttpURLConnection connection) throws IOException {
        if (connection != null) {
            int responseCode = connection.getResponseCode();
            AppLogger.info(String.format("Response code is %d for URL: %s", responseCode, connection.getURL()));
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER)
                    return getHttpConnection(new URL(connection.getHeaderField("Location")));
            }
            return connection;
        }
        return null;
    }

    private String getContent(HttpURLConnection connection) {
        if (connection != null) {

            String result = null;
            InputStream inputStream = null;

            try {
                if (connection.getResponseCode() >= 400) {
                    inputStream = connection.getErrorStream();
                } else {
                    inputStream = connection.getInputStream();
                }
            } catch (IOException e) {
                AppLogger.error(String.format("Unable to get input stream from URL: %s", connection.getURL()));
            }

            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    result = sb.toString();

                } catch (IOException e) {
                    AppLogger.error(String.format("Unable to read data from URL: %s", connection.getURL()));
                    e.printStackTrace();
                }
            }
            return result;
        }

        return null;
    }

}
