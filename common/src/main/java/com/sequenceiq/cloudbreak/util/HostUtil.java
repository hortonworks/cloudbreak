package com.sequenceiq.cloudbreak.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostUtil {

    private static final Pattern HAS_PORT_PATTERN = Pattern.compile(".*:\\d{1,5}(?:\\D.*|$)");

    private HostUtil() {

    }

    public static boolean hasPort(String url) {
        if (url == null) {
            return false;
        }
        try {
            URL parsed = new URL(url);
            return parsed.getPort() != -1;
        } catch (MalformedURLException e) {
            String message = e.getMessage();
            if (message.startsWith("no protocol") || message.startsWith("unknown protocol")) {
                Matcher matcher = HAS_PORT_PATTERN.matcher(url);
                return matcher.matches();
            }
            return false;
        }
    }
}
