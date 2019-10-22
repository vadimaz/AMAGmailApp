package com.blogspot.vadimaz.amagmailapp;

import com.blogspot.vadimaz.amagmailapp.logging.AppLogger;
import com.blogspot.vadimaz.amagmailapp.utils.GmailMessageUtils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import javax.mail.MessagingException;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GmailServiceHandler {

    private static GmailServiceHandler instance; // singleton

    private String labelId;
    private Gmail service;
    private static final String LABEL_NAME = "AMAGMAILAPP";
    public static final String USER = "me";
    private static final String APPLICATION_NAME = "AMA Gmail App";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH ="tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    private GmailServiceHandler() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        this.service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        this.labelId = initLabels(); // checking if labelId exists
    }

    public static GmailServiceHandler getInstance() throws IOException, GeneralSecurityException {
        if (instance == null) {
            instance = new GmailServiceHandler();
        }
        return instance;
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private String initLabels() throws IOException {
        if (labelId == null)  {
            ListLabelsResponse listLabelsResponse = service.users().labels().list(USER).execute();
            for (Label label : listLabelsResponse.getLabels()) {
                if (label.getName().equalsIgnoreCase(LABEL_NAME)) {
                    return label.getId();
                }
            }
            Label newLabel = new Label().setName(LABEL_NAME).setLabelListVisibility("labelShow").setMessageListVisibility("show");
            newLabel = service.users().labels().create(USER, newLabel).execute();
            return newLabel.getId();
        } else {
            return labelId;
        }
    }

    public synchronized List<Message> getMessages(String query) {
        ListMessagesResponse response;
        try {
            response = service.users().messages().list(USER).setQ(query).execute();
        } catch (Exception e) {
            AppLogger.error("Unable to read messages from server. Reconnecting...");
            e.printStackTrace();
            return getMessages(query);
        }
        if (response != null && response.size() > 0) return response.getMessages();
        else return null;
    }

    public synchronized Message getMessage(String id) {
        try {
            return service.users().messages().get(USER, id).execute();
        } catch (Exception e) {
            AppLogger.error("Unable to read message" +  id + ". Reconnecting...");
            e.printStackTrace();
            return getMessage(id);
        }
    }


    public synchronized void sendMessage(String to, String from, String subject, String message) {
        try {
            GmailMessageUtils.sendMessage(service, USER, GmailMessageUtils.createEmail(to, from, subject, message));
        } catch (Exception e) {
            AppLogger.error("Unable to send message. Reconnecting...");
            e.printStackTrace();
            sendMessage(to, from, subject, message);
        }
    }

    public synchronized void markMessageAsRead(Message message) {
        if (labelId != null) {
            ModifyMessageRequest request = new ModifyMessageRequest()
                    .setAddLabelIds(Collections.singletonList(labelId))
                    .setRemoveLabelIds(Collections.singletonList("UNREAD"));
            try {
                service.users().messages().modify(USER, message.getId(),request).execute();
            } catch (Exception e) {
                AppLogger.error("Unable to mark message" +  message.getId() + " as read. Reconnecting...");
                e.printStackTrace();
                markMessageAsRead(message);
            }
        }
    }

    public Gmail getService() {
        return service;
    }
}
