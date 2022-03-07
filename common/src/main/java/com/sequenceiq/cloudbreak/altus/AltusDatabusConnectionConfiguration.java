package com.sequenceiq.cloudbreak.altus;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("altus.databus.connection-check")
public class AltusDatabusConnectionConfiguration {

    private int maxTimeSeconds;

    private int retryTimes;

    private int retryDelaySeconds;

    private int retryMaxTimeSeconds;

    public int getMaxTimeSeconds() {
        return maxTimeSeconds;
    }

    public void setMaxTimeSeconds(int maxTimeSeconds) {
        this.maxTimeSeconds = maxTimeSeconds;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public int getRetryMaxTimeSeconds() {
        return retryMaxTimeSeconds;
    }

    public void setRetryMaxTimeSeconds(int retryMaxTimeSeconds) {
        this.retryMaxTimeSeconds = retryMaxTimeSeconds;
    }

    @Override
    public String toString() {
        return "AltusDatabusConnectionConfiguration{" +
                "maxTimeSeconds=" + maxTimeSeconds +
                ", retryTimes=" + retryTimes +
                ", retryDelaySeconds=" + retryDelaySeconds +
                ", retryMaxTimeSeconds=" + retryMaxTimeSeconds +
                '}';
    }
}
