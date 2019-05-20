package com.sequenceiq.it.cloudbreak.logsearch;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrationtest.logsearch")
public class LogSearchProps {

    private String url;

    private String components;

    private String timeRangeInterval;

    private List<QueryType> queryTypes;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<QueryType> getQueryTypes() {
        return queryTypes;
    }

    public void setQueryTypes(List<QueryType> queryTypes) {
        this.queryTypes = queryTypes;
    }

    public String getComponents() {
        return components;
    }

    public void setComponents(String components) {
        this.components = components;
    }

    public String getTimeRangeInterval() {
        return timeRangeInterval;
    }

    public void setTimeRangeInterval(String timeRangeInterval) {
        this.timeRangeInterval = timeRangeInterval;
    }
}
