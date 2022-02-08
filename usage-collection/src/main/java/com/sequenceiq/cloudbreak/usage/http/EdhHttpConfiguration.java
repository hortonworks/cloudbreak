package com.sequenceiq.cloudbreak.usage.http;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("telemetry.usage.http")
public class EdhHttpConfiguration {

    private boolean enabled;

    private String endpoint;

    private int workers;

    private int queueSizeLimit;

    private boolean forceLogging;

    private List<EdhHttpAdditionalField> additionalFields;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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

    public boolean isForceLogging() {
        return forceLogging;
    }

    public void setForceLogging(boolean forceLogging) {
        this.forceLogging = forceLogging;
    }

    public List<EdhHttpAdditionalField> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(List<EdhHttpAdditionalField> additionalFields) {
        this.additionalFields = additionalFields;
    }
}
