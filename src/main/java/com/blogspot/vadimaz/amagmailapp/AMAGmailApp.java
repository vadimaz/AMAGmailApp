package com.blogspot.vadimaz.amagmailapp;

import com.blogspot.vadimaz.amagmailapp.config.Configuration;
import com.blogspot.vadimaz.amagmailapp.logging.AppLogger;
import com.blogspot.vadimaz.amagmailapp.utils.GmailMessageUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.model.Message;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AMAGmailApp implements OrderListener {

    private int attemptCount;
    public AtomicInteger succeed = new AtomicInteger(0);
    public AtomicInteger failed  = new AtomicInteger(0);
    private Configuration config;
    private GmailServiceHandler serviceHandler;
    private static final String CONFIG_FILE_NAME = "config.json";

    public void init() {
        if (!configInit()) return;
        try {
            serviceHandler = GmailServiceHandler.getInstance();
        } catch (IOException | GeneralSecurityException e) {
            AppLogger.error("Unable to initialize the app. Quitting...");
            e.printStackTrace();
            return;
        }
        String query = GmailMessageUtils.buildQuery(config);
        AppLogger.info("Gmail searching query: " + query);
        while(true) {
            List<Message> messages = serviceHandler.getMessages(query);
            if (messages != null && messages.size() > 0) {
                for (Message message : messages) {
                    new Thread(new Order(message, serviceHandler, config,  this)).start();
                }
            }
            break; // just one iteration, delete the line after testing
        }
    }

    private boolean configInit() {
        File file = new File(CONFIG_FILE_NAME);
        if (file.exists()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                config = objectMapper.readValue(file, Configuration.class);
                return true;

            } catch (IOException e) {
                AppLogger.error("Unable to read configuration file '" + CONFIG_FILE_NAME +"'. Quitting...");
                e.printStackTrace();
                return false;
            }
        } else {
            AppLogger.error("Unable to find configuration file '" + CONFIG_FILE_NAME +"'. Quitting...");
            return false;
        }
    }

    @Override
    public synchronized void onOrderReady(Order order) {
        //AppLogger.info("Order saved."); // to do
        AppLogger.info(order.toString());
        try {
            GmailMessageUtils.sendMessage(
                    serviceHandler.getService(),
                    GmailServiceHandler.USER,
                    GmailMessageUtils.createEmail(
                            "vzorenko@gmail.com",
                            "vzorenko@gmail.com",
                            (order.isAvailable() ? "SUCCESS! " : "FAILED! ") + order.getCompany().getName(),
                            order.toString()));
        } catch (MessagingException | IOException e) {
            AppLogger.error("Unable to send message.");
            e.printStackTrace();
        }
    }
}
