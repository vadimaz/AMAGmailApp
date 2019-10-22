package com.blogspot.vadimaz.amagmailapp;

import com.blogspot.vadimaz.amagmailapp.config.Configuration;
import com.blogspot.vadimaz.amagmailapp.logging.AppLogger;
import com.blogspot.vadimaz.amagmailapp.utils.GmailMessageUtils;
import com.blogspot.vadimaz.amagmailapp.utils.URLExtractor;
import com.google.api.services.gmail.model.Message;

import java.net.URL;
import java.util.Date;
import java.util.Set;

public class Order implements Runnable {
    private Message message;
    private GmailServiceHandler serviceHandler;
    private Configuration config;
    private String offer;
    private URL url;
    private String response;
    private boolean accepted;
    private OrderListener listener;
    private Company company;

    public Order(Message message, GmailServiceHandler serviceHandler, Configuration config, OrderListener listener) {
        this.message = message;
        this.serviceHandler = serviceHandler;
        this.listener = listener;
        this.config = config;
        this.accepted = false;
    }

    @Override
    public void run() {
        try {
            message = serviceHandler.getMessage(message.getId());
            offer = GmailMessageUtils.getMessageText(message);
            url = URLExtractor.getUrl(offer, config);
            response = new OrderUrlConnection(url).getHtml();
            //accepted = !response.toLowerCase().contains("no longer available");
            accepted = !GmailMessageUtils.getPlainFromHtml(response).toLowerCase().contains("no longer available");
            company = getOrderCompany();
            if (response != null) listener.onOrderReady(this);

        } catch (Exception e) {
            AppLogger.error("Unable to get and proceed message with id: " + message.getId());
            e.printStackTrace();
            run();
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
        if (response != null) {
            StringBuilder sb = new StringBuilder("\n");
            sb.append(accepted ? "Congratulations! You've got a work order from " : "Sorry, but you've missed a work offer from ");
            sb.append(company.getName()).append("\n\n");
            sb.append("Date:\n").append(new Date()).append("\n\n");
            sb.append("Offer:\n").append(offer).append("\n\n");
            sb.append("Response:\n").append(response).append("\n\n");
            return sb.toString();
        } else return super.toString();
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Company getCompany() {
        return company;
    }

    public Message getMessage() {
        return message;
    }
}
