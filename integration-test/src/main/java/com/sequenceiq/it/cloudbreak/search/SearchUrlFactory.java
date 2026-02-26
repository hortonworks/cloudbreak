package com.sequenceiq.it.cloudbreak.search;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SearchUrlFactory {

    private static SplunkProps splunkProps;

    public static boolean isSplunkConfigured() {
        return splunkProps != null && StringUtils.hasText(splunkProps.getUrl());
    }

    /**
     * Create a SearchUrl instance based on configuration.
     * If Splunk URL is configured, returns SplunkSearchUrl, otherwise returns KibanaSearchUrl.
     * 
     * @return SearchUrl implementation based on configuration
     */
    public static SearchUrl getSearchUrl() {
        if (isSplunkConfigured()) {
            return new SplunkSearchUrl();
        } else {
            return new KibanaSearchUrl();
        }
    }

    @Inject
    public void setSplunkProps(SplunkProps splunkProps) {
        SearchUrlFactory.splunkProps = splunkProps;
    }

}
