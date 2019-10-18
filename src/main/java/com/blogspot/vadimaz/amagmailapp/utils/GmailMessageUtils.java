package com.blogspot.vadimaz.amagmailapp.utils;

import com.blogspot.vadimaz.amagmailapp.config.Configuration;
import com.blogspot.vadimaz.amagmailapp.Company;
import com.blogspot.vadimaz.amagmailapp.logging.AppLogger;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import org.jsoup.Jsoup;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

public class GmailMessageUtils {

    public static String getMessageText(Message message) {
        String result = StringUtils.newStringUtf8(message.getPayload().getBody().decodeData());
        if (result == null) {
            result = StringUtils.newStringUtf8(message.getPayload().getParts().get(1).getBody().decodeData());
        }
        return result;
    }

    public static String buildQuery(Configuration config) {
        StringBuilder queryBuilder = new StringBuilder("in:inbox is:unread newer_than:1d");
        if (config.getCompanies().length > 0) {
            StringBuilder from = new StringBuilder(" {");
            StringBuilder subject = new StringBuilder(" {");
            for (Company company : config.getCompanies()) {
                from.append("from:").append(company.getEmail().toLowerCase()).append(" ");
                subject.append("subject:").append("(").append(company.getSubject().toLowerCase()).append(")").append( " ");
            }
            from.deleteCharAt(from.length()-1).append("}");
            subject.deleteCharAt(subject.length()-1).append("}");
            queryBuilder.append(from).append(subject);
        }
        if (config.getZips().length > 0) {
            StringBuilder zips = new StringBuilder(" {");
            for (String zip: config.getZips()) {
                zips.append("\"").append(zip).append("\" ");
            }
            zips.deleteCharAt(zips.length()-1).append("}");
            queryBuilder.append(zips);
        }
        return queryBuilder.toString();
    }

    public static String getPlainFromHtml(String html) {
        return Jsoup.parse(html).body().text();
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param to email address of the receiver
     * @param from email address of the sender, the mailbox account
     * @param subject subject of the email
     * @param bodyText body text of the email
     * @return the MimeMessage to be used to send email
     * @throws MessagingException
     */
    public static MimeMessage createEmail(String to,
                                          String from,
                                          String subject,
                                          String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText); // sends a plain text
        //email.setText(bodyText, "utf-8", "html"); // sends a html
        return email;
    }

    /**
     * Create a message from an email.
     *
     * @param emailContent Email to be set to raw of message
     * @return a message containing a base64url encoded email
     * @throws IOException
     * @throws MessagingException
     */
    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     * Send an email from the user's mailbox to its recipient.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param emailContent Email to be sent.
     * @return The sent message
     * @throws MessagingException
     * @throws IOException
     */
    public static Message sendMessage(Gmail service,
                                      String userId,
                                      MimeMessage emailContent)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();
        AppLogger.info(String.format("Message '%s' sent to '%s'", emailContent.getSubject(), emailContent.getAllRecipients()[0].toString()));
        return message;
    }
}
