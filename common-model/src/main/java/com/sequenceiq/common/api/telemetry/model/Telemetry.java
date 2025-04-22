package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Telemetry implements Serializable {

    @JsonProperty("logging")
    private Logging logging;

    @JsonProperty("workloadAnalytics")
    private WorkloadAnalytics workloadAnalytics;

    @JsonProperty("monitoring")
    private Monitoring monitoring;

    @JsonProperty("databusEndpoint")
    private String databusEndpoint;

    @JsonProperty("features")
    private Features features;

    @JsonProperty("fluentAttributes")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> fluentAttributes = new HashMap<>();

    @JsonProperty("dynamicEntitlements")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Boolean> dynamicEntitlements = new HashMap<>();

    @JsonProperty("rules")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AnonymizationRule> rules = new ArrayList<>();

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(Monitoring monitoring) {
        this.monitoring = monitoring;
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

    public Map<String, Boolean> getDynamicEntitlements() {
        return dynamicEntitlements;
    }

    public void setDynamicEntitlements(Map<String, Boolean> dynamicEntitlements) {
        this.dynamicEntitlements = dynamicEntitlements;
    }

    public List<AnonymizationRule> getRules() {
        return rules;
    }

    public void setRules(List<AnonymizationRule> rules) {
        this.rules = rules;
    }

    @JsonIgnore
    public boolean isMonitoringFeatureEnabled() {
        return features == null || features.getMonitoring() != null
                && features.getMonitoring().getEnabled();
    }

    @JsonIgnore
    public boolean isUseSharedAltusCredentialEnabled() {
        return features != null && features.getUseSharedAltusCredential() != null
                && features.getUseSharedAltusCredential().getEnabled();
    }

    @JsonIgnore
    public boolean isComputeMonitoringEnabled() {
        return !Objects.isNull(monitoring) && StringUtils.isNotBlank(monitoring.getRemoteWriteUrl());
    }

    @JsonIgnore
    public boolean isCloudStorageLoggingEnabled() {
        return features == null || features.getCloudStorageLogging() != null
                && features.getCloudStorageLogging().getEnabled();
    }

}
