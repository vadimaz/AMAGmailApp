package com.blogspot.vadimaz.amagmailapp;

import com.blogspot.vadimaz.amagmailapp.config.Configuration;
import com.blogspot.vadimaz.amagmailapp.logging.AppLogger;
import com.blogspot.vadimaz.amagmailapp.utils.GmailMessageUtils;
import com.blogspot.vadimaz.amagmailapp.utils.URLExtractor;
import com.google.api.services.gmail.model.Message;

import java.net.URL;
import java.util.Date;

public class Order implements Runnable {
    private Message message;
    private GmailServiceHandler serviceHandler;
    private Configuration config;
    private String offer;
    private URL url;
    private String response;
    private boolean available;
    private OrderListener listener;
    private Company company;
    private boolean orderReady;
    private static int connectionAttempt = 0;

    public Order(Message message, GmailServiceHandler serviceHandler, Configuration config,  OrderListener listener) {
        this.message = message;
        this.serviceHandler = serviceHandler;
        this.listener = listener;
        this.config = config;
        this.orderReady = false;
        this.available = false;
    }

    @Override
    public void run() {
        connectionAttempt++;
        try {
            message = serviceHandler.getMessage(message.getId());
            String messageText = GmailMessageUtils.getMessageText(message);
            offer = GmailMessageUtils.getPlainFromHtml(messageText);
            url = URLExtractor.getUrl(messageText, config);
            response = GmailMessageUtils.getPlainFromHtml(new OrderUrlConnection(url).getHtml());
            available = !response.toLowerCase().contains("no longer available");
            company = getOrderCompany();
            orderReady = response != null;
            if (orderReady) listener.onOrderReady(this);

        } catch (Exception e) {
            AppLogger.error("Unable to get and proceed message with id: " + message.getId());
            e.printStackTrace();
            //if(connectionAttempt < 4) run();
        }
    }

    private Company getOrderCompany() {
        for (Company company : config.getCompanies()) {
            if (url.toString().toLowerCase().contains(company.getTriggers()[0].toLowerCase())) return company;
        }
        return null;
    }

    @Override
    public String toString() {
        if (orderReady) {
            StringBuilder sb = new StringBuilder("\n");
            sb.append(available ? "SUCCESS! ORDER IS AVAILABLE." : "FAILED! ORDER IS NOT AVAILABLE!").append("\n\n");
            sb.append("Company:\n").append(company.getName()).append("\n\n");
            sb.append("Date:\n").append(new Date()).append("\n\n");
            sb.append("Offer:\n").append(offer).append("\n\n");
            sb.append("Response:\n").append(response).append("\n\n");
            return sb.toString();
        } else return super.toString();
    }

    public boolean isAvailable() {
        return available;
    }

    public Company getCompany() {
        return company;
    }
}
