package com.sequenceiq.it.cloudbreak.search;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@EnableConfigurationProperties(SplunkProps.class)
public class SplunkSearchUrl implements SearchUrl {

    private static final String RESOURCE_NAME_FIELD = "context.resourceName";

    private static SplunkProps splunkProps;

    private List<Searchable> searchable;

    private Date testStartDate;

    private Date testStopDate;

    private void splunkSearchUrl(List<Searchable> searchable, Date testStartDate, Date testStopDate) {
        if (searchable.isEmpty()) {
            throw new IllegalArgumentException("searchable must be non empty");
        }
        if (testStartDate == null || testStopDate == null) {
            throw new IllegalArgumentException("Dates must be filled");
        }

        this.searchable = searchable;
        this.testStartDate = testStartDate;
        this.testStopDate = testStopDate;
    }

    @Override
    public String getSearchUrl(List<Searchable> searchable, Date testStartDate, Date testStopDate) {
        splunkSearchUrl(searchable, testStartDate, testStopDate);
        return buildSplunkUrl();
    }

    @Inject
    @SuppressFBWarnings("ST")
    public void setSplunkProps(SplunkProps splunkProps) {
        this.splunkProps = splunkProps;
    }

    private String buildSplunkUrl() {
        try {
            // Convert dates to Unix timestamps (seconds since epoch)
            long earliestTime = testStartDate.getTime() / 1000;
            long latestTime = testStopDate.getTime() / 1000;

            String searchQuery = buildSplunkSearchQuery();
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            return String.format("%s?q=%s&earliest=%d&latest=%d",
                    SplunkSearchUrl.splunkProps.getUrl(),
                    encodedQuery,
                    earliestTime,
                    latestTime);
        } catch (Exception ex) {
            throw new RuntimeException("Error generating splunk search URl", ex);
        }
    }

    private String buildSplunkSearchQuery() {

        String conditions = searchable.stream()
                .map(s -> String.format("%s=%s", RESOURCE_NAME_FIELD, s.getSearchId()))
                .collect(Collectors.joining(" OR "));

        return String.format("search index=%s (%s) | table @message @app %s | head 1000",
                splunkProps.getIndex(), conditions, RESOURCE_NAME_FIELD);
    }
}
