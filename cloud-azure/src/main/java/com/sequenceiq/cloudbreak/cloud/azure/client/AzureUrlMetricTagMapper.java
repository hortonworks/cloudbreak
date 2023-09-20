package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Request;

public class AzureUrlMetricTagMapper implements Function<Request, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUrlMetricTagMapper.class);

    private static final String DEFAULT_TAG = "none";

    private static final Pattern PROVIDER_PATTERN = Pattern.compile("/providers/(.*?)/");

    @Override
    public String apply(Request request) {
        if (request != null && request.url() != null) {
            String url = request.url().toString();
            try {
                return mapRequestUrlToMetricTag(url);
            } catch (Exception e) {
                LOGGER.warn("Cannot map Azure url to metric tag: {}", url, e);
            }
        }
        return DEFAULT_TAG;
    }

    private String mapRequestUrlToMetricTag(String url) {
        if (url.contains("login.microsoftonline.com")) {
            return "login";
        } else if (url.contains("/providers/")) {
            Matcher matcher = PROVIDER_PATTERN.matcher(url);
            if (matcher.find()) {
                return matcher.group(1).toLowerCase(Locale.ROOT);
            }
        } else if (url.contains("/operationresults/")) {
            return "operationresults";
        } else if (url.contains("/resourcegroups/")) {
            return "resourcegroups";
        }
        return DEFAULT_TAG;
    }
}
