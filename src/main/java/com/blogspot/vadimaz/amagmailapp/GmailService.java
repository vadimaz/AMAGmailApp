package com.blogspot.vadimaz.amagmailapp;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.StringUtils;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import java.io.*;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GmailService {

    public static final String USER = "me";

    private static final String APPLICATION_NAME = "AMA Gmail App";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH ="tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GmailService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private Gmail service;
    private String[] zips;
    private Company company;
    private static final String LABEL_NAME = "AMAGMAILAPP";
    private static String labelId;

    public GmailService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        this.service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        initLabels(); // checking if labelId exists
    }

    public void setZips(String[] zips) {
        this.zips = zips;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public List<URL> getNewURLs() throws IOException {
        List<Message> messages = getMessages();
        if (messages == null) return null;
        List<URL> result = new ArrayList<>();
        for (Message message : messages) {
            String urlString = getAppropriateURLString(extractURLStrings(message));
            if (urlString != null) result.add(new URL(urlString));
            markMessageAsReadAndChangeItsLabel(message);
        }
        return result;
    }

    private void markMessageAsReadAndChangeItsLabel(Message message) throws IOException {
        if (labelId != null) {
            ModifyMessageRequest request = new ModifyMessageRequest().setAddLabelIds(Collections.singletonList(labelId)).setRemoveLabelIds(Collections.singletonList("UNREAD"));
            //ModifyMessageRequest request = new ModifyMessageRequest().setAddLabelIds(Collections.singletonList(labelId));
            service.users().messages().modify(USER, message.getId(),request).execute();
        }
    }

    private List<Message> getMessagesBySubject(List<Message> messages, String subject) throws IOException {
        if (messages == null || messages.size() == 0) return null;
        List<Message> result = new ArrayList<>();
        for (Message message : messages) {
            String messageSubject = message.getPayload().getHeaders().get(findSubjectId(message)).getValue();
            //System.out.println("Inside getMessagesBySubject(): messageSubject is " + messageSubject); // to delete
            if (messageSubject.toLowerCase().contains(subject.toLowerCase())) {
                result.add(message);
            }
        }

        return result;
    }
    private List<Message> getMessagesByZip(List<Message> messages) throws IOException {
        if (messages == null || messages.size() == 0) return null;
        List<Message> result = new ArrayList<>();
        for (Message message : messages) {
            if (zips != null) {
                String messageText = getMessageText(message);
                //System.out.println(messageText); // to delete
                if (containsZip(messageText)) result.add(message);
            } else {
                result.add(message);
            }
        }
        return result;
    }

    private List<Message> getMessages() throws IOException {
        ListMessagesResponse messagesResponse = service.users().messages().list(USER).setQ(String.format("in:inbox from:%s is:unread", company.getEmail())).execute();
        if (messagesResponse.getMessages() == null) return null;
        //System.out.println("There are " + messagesResponse.getMessages().size() + " new messages."); // to delete
        List<Message> messages = new ArrayList<>();
        for (Message m : messagesResponse.getMessages()) {
            Message message = getMessage(m.getId());
            messages.add(message);
        }
        return getMessagesByZip(getMessagesBySubject(messages, company.getSubject()));
    }

    private Message getMessage(String id) throws IOException {
        return service.users().messages().get(USER, id).execute();
    }

    private static String getMessageText(Message message) {
        byte[] data = new byte[0];
        String result = null;
        try {
            result = StringUtils.newStringUtf8(message.getPayload().getBody().decodeData());
            //System.out.println(result);
            //data = message.getPayload().getParts().get(0).getBody().decodeData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<String> extractURLStrings(Message message) {
        List<String> result = new ArrayList<>();
        String messageText = getMessageText(message);
        if (messageText == null) return result;

        // Pattern for recognizing a URL
        Pattern urlPattern = Pattern.compile(
                "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = urlPattern.matcher(messageText);
        while (matcher.find()) {
            int matchStart = matcher.start(1);
            int matchEnd = matcher.end();
            result.add(messageText.substring(matchStart, matchEnd));
        }

        return result;
    }

    private String getAppropriateURLString(List<String> urls) {
        for (String url: urls) {
            boolean contains = false;
            for (String trigger : company.getTriggers()) {
                contains = url.contains(trigger);
                if (!contains) break;
            }
            if (contains) return url;
        }
        return null;
    }

    private boolean containsZip(String messageText) {
        if (messageText != null) {
            for (String zip : zips) {
                if (messageText.contains(zip)) return true;
            }
        }
        return false;
    }

    public Gmail getService() {
        return service;
    }

    private void initLabels() throws IOException {
        if (labelId == null)  {
            ListLabelsResponse listLabelsResponse = service.users().labels().list("me").execute();
            for (Label label : listLabelsResponse.getLabels()) {
                if (label.getName().equalsIgnoreCase(LABEL_NAME)) {
                    labelId = label.getId();
                    return;
                }
            }
            Label newLabel = new Label().setName(LABEL_NAME).setLabelListVisibility("labelShow").setMessageListVisibility("show");
            newLabel = service.users().labels().create(USER, newLabel).execute();
            labelId = newLabel.getId();
        }
    }
    private int findSubjectId(Message message) {
        List<MessagePartHeader> headerList = message.getPayload().getHeaders();
        for (int i = 0; i < headerList.size(); i++) {
            MessagePartHeader partHeader = headerList.get(i);
            if (partHeader.getName().equalsIgnoreCase("subject")) {
                return i;
            }
        }
        return 0;
    }
}
