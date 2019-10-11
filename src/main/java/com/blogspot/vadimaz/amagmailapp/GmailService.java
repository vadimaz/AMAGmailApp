package com.blogspot.vadimaz.amagmailapp;

import com.blogspot.vadimaz.amagmailapp.config.Configuration;
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
import org.apache.commons.text.StringEscapeUtils;

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
        //InputStream in = GmailService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        InputStream in = new FileInputStream("credentials.json");
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
    private Configuration config;
    private static final String LABEL_NAME = "AMAGMAILAPP";
    private static String labelId;

    public GmailService(Configuration config) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        this.service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        this.config = config;
        initLabels(); // checking if labelId exists
    }

    public  List<URL> getUrls(List<Message> messages) throws IOException {
        if (messages != null && messages.size() > 0) {
            List<URL> urls = new ArrayList<>();
            for (Message message : messages) {
                String urlString = getAppropriateURLString(extractURLStrings(message));
                if (urlString != null) urls.add(new URL(StringEscapeUtils.unescapeXml(urlString)));
                markMessageAsReadAndChangeItsLabel(message);
            }
            return urls;
        }
        return null;
    }

    private void markMessageAsReadAndChangeItsLabel(Message message) throws IOException {
        if (labelId != null) {
            ModifyMessageRequest request = new ModifyMessageRequest().setAddLabelIds(Collections.singletonList(labelId)).setRemoveLabelIds(Collections.singletonList("UNREAD"));
            service.users().messages().modify(USER, message.getId(),request).execute();
        }
    }

    private List<Message> getMessagesBySubject(List<Message> messages, String subject) throws IOException {
        if (messages == null || messages.size() == 0) return null;
        List<Message> result = new ArrayList<>();
        for (Message message : messages) {
            String messageSubject = message.getPayload().getHeaders().get(findSubjectId(message)).getValue();
            if (messageSubject.toLowerCase().contains(subject.toLowerCase())) {
                result.add(message);
            }
        }

        return result;
    }
    private List<Message> getMessagesByZip(List<Message> messages) throws IOException {
        if (messages != null && messages.size() > 0) {
            List<Message> result = new ArrayList<>();
            for (Message message : messages) {
                if (config.getZips() != null) {
                    String messageText = getMessageText(message);
                    if (containsZip(messageText)) result.add(message);
                } else {
                    result.add(message);
                }
            }
            return result;
        }
        return null;
    }

    public List<Message> getMessages(String query) throws IOException {
        if (query != null) {
            ListMessagesResponse messagesResponse = service.users().messages().list(USER).setQ(query).execute();
            //System.out.println(messagesResponse.getMessages() != null ? messagesResponse.getMessages().size() : null);
            if (messagesResponse.getMessages() != null && messagesResponse.getMessages().size() > 0) {
                List<Message> messages = new ArrayList<>();
                for (Message m : messagesResponse.getMessages()) {
                    Message message = getMessage(m.getId());
                    messages.add(message);
                }
                return getMessagesByZip(messages);
            }
        }
        return null;
    }

    private Message getMessage(String id) throws IOException {
        return service.users().messages().get(USER, id).execute();
    }

    private static String getMessageText(Message message) {
        byte[] data = new byte[0];
        String result = null;
        try {
            result = StringUtils.newStringUtf8(message.getPayload().getBody().decodeData());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<String> extractURLStrings(Message message) {
        List<String> result = new ArrayList<>();
        String messageText = getMessageText(message);
        if (messageText == null || messageText.length() == 0) return result;

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
            for (Company company : config.getCompanies()) {
                for (String trigger : company.getTriggers()) {
                    contains = url.contains(trigger);
                    if (!contains) break;
                }
                if (contains) return url;
            }
        }
        return null;
    }

    private boolean containsZip(String messageText) {
        if (messageText != null) {
            for (String zip : config.getZips()) {
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
