package com.blogspot.vadimaz.amagmailapp;

import org.apache.commons.text.StringEscapeUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OrderUrlConnection {
    private URL url;
    private String html;

    public OrderUrlConnection(URL url) throws IOException {
        this.url = url;
        this.html = null;
        init();
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
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER)
                    return getHttpConnection(new URL(StringEscapeUtils.unescapeXml(connection.getHeaderField("Location"))));
            }
            return connection;
        }
        return null;
    }

    private String getContent(HttpURLConnection connection) throws IOException {
        if (connection != null) {

            String result = null;
            InputStream inputStream = null;

            if (connection.getResponseCode() >= 400) {
                inputStream = connection.getErrorStream();
            } else {
                inputStream = connection.getInputStream();
            }

            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    result = sb.toString();

                }
            }
            return result;
        }
        return null;
    }

    public String getHtml() {
        return html;
    }

    private void init() throws IOException {
        HttpURLConnection connection = getHttpConnection(url); // getting http or https url connection
        if (connection != null) html = getContent(connection); // reading html from connection
    }
}
