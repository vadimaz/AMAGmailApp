package com.blogspot.vadimaz.amagmailapp;

import com.google.api.services.gmail.Gmail;

import javax.mail.MessagingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        try {
            URLConnection connection = url.openConnection();
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
        }

    }
}
