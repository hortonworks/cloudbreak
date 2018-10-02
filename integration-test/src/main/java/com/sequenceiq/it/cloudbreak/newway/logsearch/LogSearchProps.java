package com.sequenceiq.it.cloudbreak.newway.logsearch;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrationtest.logsearch")
public class LogSearchProps {

    private String url;

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
}
