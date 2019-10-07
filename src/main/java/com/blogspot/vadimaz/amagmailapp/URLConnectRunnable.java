package com.blogspot.vadimaz.amagmailapp;

import com.google.api.services.gmail.Gmail;

import javax.mail.MessagingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class URLConnectRunnable implements Runnable {
    private URL url;
    private Gmail service;
    private Company company;
    private MailSender mailSender;
    private static final String TO = "vzorenko@gmail.com";
    private static final String FROM = "info@eliterestorationteam.com";
    private String subject;
    public URLConnectRunnable(URL url, Gmail service, Company company) {
        this.url = url;
        this.service = service;
        this.company = company;
        this.mailSender = new MailSender();
        this.subject = company.getName() + ": " + url;
    }

    @Override
    public void run() {
        /*try {
            URLConnection connection = url.openConnection();
            connection.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder page = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                page.append(line);
            }
            reader.close();
            System.out.println(page);
            MailSender.sendMessage(service, GmailService.USER, MailSender.createEmail(TO, FROM, subject, page.toString()));
        } catch (IOException e) {
            System.out.println("Can't connect to " + url);
        } catch (MessagingException e) {
            System.out.println("Can't send the mail '" + company.getName() + ": " + url + "'");
        }*/

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(5000);
            connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            connection.addRequestProperty("User-Agent", "Mozilla");
            connection.addRequestProperty("Referer", "google.com");
            System.out.println("Request URL: " + url);

            boolean redirect = false;
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER)
                    redirect = true;
            }

            System.out.println("Responce Code: " + status);

            if (redirect) {
                String newUrl = connection.getHeaderField("Location");
                String cookies = connection.getHeaderField("Set-Cookie");
                connection = (HttpURLConnection) new URL(newUrl).openConnection();
                connection.setRequestProperty("Cookie", cookies);
                connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                connection.addRequestProperty("User-Agent", "Mozilla");
                connection.addRequestProperty("Referer", "google.com");

                System.out.println("Redirect to URL: " + newUrl);
            }

        } catch (IOException e) {
            System.out.println("Can't open connection with " + url);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder page = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                page.append(line);
            }
            MailSender.sendMessage(service, GmailService.USER, MailSender.createEmail(TO, FROM, subject, page.toString()));
        } catch (IOException e) {
            System.out.println("Can't read data from " + url);
        } catch (MessagingException e) {
            System.out.println("Can't send the mail '" + company.getName() + ": " + url + "'");
        }

    }
}
