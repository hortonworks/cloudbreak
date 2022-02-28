package com.sequenceiq.cloudbreak.usage.processor;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("telemetry.usage.cloudwatch")
public class EdhCloudwatchConfiguration {

    private boolean enabled;

    private int workers;

    private int queueSizeLimit;

    private String logGroup;

    private String logStream;

    private String region;

    private int maxRetry;

    private boolean forceLogging;

    private List<EdhCloudwatchField> additionalFields;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWorkers() {
        return workers;
    }

    public void setWorkers(int workers) {
        this.workers = workers;
    }

    public int getQueueSizeLimit() {
        return queueSizeLimit;
    }

    public void setQueueSizeLimit(int queueSizeLimit) {
        this.queueSizeLimit = queueSizeLimit;
    }

    public String getLogGroup() {
        return logGroup;
    }

    public void setLogGroup(String logGroup) {
        this.logGroup = logGroup;
    }

    public String getLogStream() {
        return logStream;
    }

    public void setLogStream(String logStream) {
        this.logStream = logStream;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isForceLogging() {
        return forceLogging;
    }

    public void setForceLogging(boolean forceLogging) {
        this.forceLogging = forceLogging;
    }

    public List<EdhCloudwatchField> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(List<EdhCloudwatchField> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }
}
