package com.sequenceiq.cloudbreak.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class URLUtils {

    private URLUtils() {
    }

    public static String readUrl(String url) throws IOException {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return IOUtils.toString(new URL(url));
    }

    public static String encodeString(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

}
