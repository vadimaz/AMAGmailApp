package com.blogspot.vadimaz.amagmailapp;

import com.blogspot.vadimaz.amagmailapp.config.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.model.Message;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AMAGmailApp {

    private static int attemptCount;
    public static AtomicInteger succeed = new AtomicInteger(0);
    public static AtomicInteger failed  = new AtomicInteger(0);

    public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException {

        ObjectMapper objectMapper = new ObjectMapper();
        File configFile = new File("config.json");
        if (!configFile.exists()) {
            AppLogger.error("Unable to find configuration file 'config.json'. Quitting...");
            return;
        }
        Configuration config = objectMapper.readValue(configFile, Configuration.class);
        GmailService service = new GmailService(config);
        AppLogger.info("App is configured. Start working ...");

        while (true) {
            attemptCount++;
            AppLogger.info(String.format("Attempt #%d:", attemptCount));
            int attemptOrders = 0;
            for (Company company : config.getCompanies()) {
                String query = String.format("in:inbox is:unread subject:%s", company.getSubject());
                List<Message> messages = null;
                try {
                    messages = service.getMessages(query);
                } catch (Exception e) {
                    AppLogger.error("Unable to get messages from the server.");
                    e.printStackTrace();
                }
                List<URL> urls = service.getUrls(messages);

                if (urls != null && urls.size() > 0) {
                    attemptOrders += urls.size();
                    for (URL url : urls) {
                        AppLogger.info(String.format("URL extracted: %s", url.toString()));
                        new Thread(new URLConnectRunnable(url, service.getService())).start();
                    }

                }
            }
            AppLogger.info(String.format("Order(s) found: %d", attemptOrders));
            if (attemptCount%10 == 0) {
                int totalOrders = succeed.get() + failed.get();
                AppLogger.info(String.format("TOTAL ORDERS PROCEED: %d", totalOrders));
                if (totalOrders > 0) AppLogger.info(String.format("SUCCEED: %d, FAILED: %d", succeed.get(), failed.get()));
            }

            Thread.sleep(1000); // delay
        }

    }
}
