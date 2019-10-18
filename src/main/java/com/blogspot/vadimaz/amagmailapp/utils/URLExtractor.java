package com.blogspot.vadimaz.amagmailapp.utils;

import com.blogspot.vadimaz.amagmailapp.config.Configuration;
import com.blogspot.vadimaz.amagmailapp.Company;
import org.apache.commons.text.StringEscapeUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLExtractor {
    public static URL getUrl(String messageText, Configuration config) throws MalformedURLException {
        if (messageText != null && messageText.length() > 0) {
            // Pattern for recognizing a URL
            Pattern urlPattern = Pattern.compile(
                    "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = urlPattern.matcher(messageText);
            while (matcher.find()) {
                int matchStart = matcher.start(1);
                int matchEnd = matcher.end();
                String urlString = messageText.substring(matchStart, matchEnd);
                for(Company company : config.getCompanies()) {
                    if (urlStringContainsTriggers(urlString, company.getTriggers())) {
                        return new URL(StringEscapeUtils.unescapeXml(urlString));
                    }
                }
            }
        }
        return null;
    }

    private static boolean urlStringContainsTriggers(String urlString, String[] triggers) {
        boolean contains = false;
        if (urlString != null && urlString.length() > 0 && triggers != null && triggers.length > 0) {
            for (String trigger : triggers) {
                contains = urlString.toLowerCase().contains(trigger.toLowerCase());
                if (!contains) break;
            }
        }
        return contains;
    }
}
