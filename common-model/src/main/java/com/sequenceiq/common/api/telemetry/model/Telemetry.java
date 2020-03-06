package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Telemetry implements Serializable {

    @JsonProperty("logging")
    private Logging logging;

    @JsonProperty("workloadAnalytics")
    private WorkloadAnalytics workloadAnalytics;

    @JsonProperty("databusEndpoint")
    private String databusEndpoint;

    @JsonProperty("features")
    private Features features;

    @JsonProperty("fluentAttributes")
    private Map<String, Object> fluentAttributes = new HashMap<>();

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public WorkloadAnalytics getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(WorkloadAnalytics workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }

    public String getDatabusEndpoint() {
        return databusEndpoint;
    }

    public void setDatabusEndpoint(String databusEndpoint) {
        this.databusEndpoint = databusEndpoint;
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

    public Map<String, Object> getFluentAttributes() {
        return fluentAttributes;
    }

    public void setFluentAttributes(Map<String, Object> fluentAttributes) {
        this.fluentAttributes = fluentAttributes;
    }

    @JsonIgnore
    public boolean isMeteringFeatureEnabled() {
        return features != null && features.getMetering() != null
                && features.getMetering().isEnabled();
    }

    @JsonIgnore
    public boolean isMonitoringFeatureEnabled() {
        return features != null && features.getMonitoring() != null
                && features.getMonitoring().isEnabled();
    }

    @JsonIgnore
    public boolean isClusterLogsCollectionEnabled() {
        return features != null && features.getClusterLogsCollection() != null
                && features.getClusterLogsCollection().isEnabled();
    }

    @JsonIgnore
    public boolean isUseSharedAltusCredentialEnabled() {
        return features != null && features.getUseSharedAltusCredential() != null
                && features.getUseSharedAltusCredential().isEnabled();
    }

}
