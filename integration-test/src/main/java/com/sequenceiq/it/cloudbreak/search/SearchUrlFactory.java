package com.sequenceiq.it.cloudbreak.search;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class SearchUrlFactory {

    private static SplunkProps splunkProps;

    /**
     * Create a SearchUrl instance based on configuration.
     * If Splunk URL is configured, returns SplunkSearchUrl, otherwise returns KibanaSearchUrl.
     * 
     * @return SearchUrl implementation based on configuration
     */
    public static SearchUrl getSearchUrl() {
        if (splunkProps != null && StringUtils.hasText(splunkProps.getUrl())) {
            return new SplunkSearchUrl();
        } else {
            return new KibanaSearchUrl();
        }
    }

    @Inject
    @SuppressFBWarnings("ST")
    public void setSplunkProps(SplunkProps splunkProps) {
        this.splunkProps = splunkProps;
    }

}
