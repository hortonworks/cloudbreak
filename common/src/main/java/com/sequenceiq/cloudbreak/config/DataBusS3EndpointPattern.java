package com.sequenceiq.cloudbreak.config;

public class DataBusS3EndpointPattern {

    private String pattern;

    private String endpoint;

    private String fipsEndpoint;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getFipsEndpoint() {
        return fipsEndpoint;
    }

    public void setFipsEndpoint(String fipsEndpoint) {
        this.fipsEndpoint = fipsEndpoint;
    }
}
