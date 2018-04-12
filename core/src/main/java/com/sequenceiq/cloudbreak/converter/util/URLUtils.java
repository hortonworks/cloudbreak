package com.sequenceiq.cloudbreak.converter.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;

public class URLUtils {

    private URLUtils() {
    }

    public static String readUrl(String url) throws IOException {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return IOUtils.toString(new URL(url));
    }
}
