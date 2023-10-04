package com.sequenceiq.mock.config;

public class ConfigParams {

    private String uriPattern;

    private String delay;

    private Integer errorRate;

    public String getUriPattern() {
        return uriPattern;
    }

    public void setUriPattern(String uriPattern) {
        this.uriPattern = uriPattern;
    }

    public String getDelay() {
        return delay;
    }

    public void setDelay(String delay) {
        this.delay = delay;
    }

    public Integer getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Integer failureRate) {
        this.errorRate = failureRate;
    }
}
