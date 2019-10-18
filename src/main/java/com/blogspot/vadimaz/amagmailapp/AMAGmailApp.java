package com.blogspot.vadimaz.amagmailapp;

import com.blogspot.vadimaz.amagmailapp.config.Configuration;
import com.blogspot.vadimaz.amagmailapp.logging.AppLogger;
import com.blogspot.vadimaz.amagmailapp.utils.GmailMessageUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.model.Message;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AMAGmailApp implements OrderListener {

    private volatile int succeed, failed;
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
        int attemptCount = failed = succeed = 0;
        Set<String> messageIds = Collections.synchronizedSet(new HashSet<>());

        while(true) {
            attemptCount++;
            List<Message> messages = serviceHandler.getMessages(query);
            if (messages != null && messages.size() > 0) {
                AppLogger.info(String.format("Attempt #%d: %d offer(s) received", attemptCount, messages.size()));
                for (Message message : messages) {
                    if (!messageIds.contains(message.getId())) {
                        messageIds.add(message.getId());
                        new Thread(new Order(message, messageIds, serviceHandler, config,  this)).start();
                    }
                }
            } else {
                AppLogger.info(String.format("Attempt #%d: offers not found", attemptCount));
            }
            if (attemptCount % 20 == 0) {
                AppLogger.info(String.format("Total offers received: %d", succeed + failed));
                AppLogger.info(String.format("Accepted: %d, missed: %d", succeed, failed));
            }
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
        serviceHandler.markMessageAsRead(order.getMessage());
        if (order.isAccepted()) succeed++;
        else failed++;
        AppLogger.info(order.toString());
        serviceHandler.sendMessage(
                "vzorenko@gmail.com",
                "info@eliterestorationteam.com",
                (order.isAccepted() ? "Congratulations! You've got a work order from " : "Sorry, but you've missed a work order offer from ") + order.getCompany().getName(),
                order.toString());
    }
}
