package com.sequenceiq.environment.environment.dto.telemetry;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.type.FeatureSetting;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentFeatures implements Serializable {

    private FeatureSetting workloadAnalytics;

    private FeatureSetting useSharedAltusCredential;

    private FeatureSetting monitoring;

    private FeatureSetting cloudStorageLogging;

    public FeatureSetting getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(FeatureSetting workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }

    public FeatureSetting getUseSharedAltusCredential() {
        return useSharedAltusCredential;
    }

    public void setUseSharedAltusCredential(FeatureSetting useSharedAltusCredential) {
        this.useSharedAltusCredential = useSharedAltusCredential;
    }

    public FeatureSetting getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(FeatureSetting monitoring) {
        this.monitoring = monitoring;
    }

    public FeatureSetting getCloudStorageLogging() {
        return cloudStorageLogging;
    }

    public void setCloudStorageLogging(FeatureSetting cloudStorageLogging) {
        this.cloudStorageLogging = cloudStorageLogging;
    }

    @JsonIgnore
    public void addWorkloadAnalytics(boolean enabled) {
        workloadAnalytics = new FeatureSetting();
        workloadAnalytics.setEnabled(enabled);
    }

    @JsonIgnore
    public void addMonitoring(boolean enabled) {
        monitoring = new FeatureSetting();
        monitoring.setEnabled(enabled);
    }

    @JsonIgnore
    public void addUseSharedAltusredential(boolean enabled) {
        useSharedAltusCredential = new FeatureSetting();
        useSharedAltusCredential.setEnabled(enabled);
    }

    @JsonIgnore
    public void addCloudStorageLogging(boolean enabled) {
        cloudStorageLogging = new FeatureSetting();
        cloudStorageLogging.setEnabled(enabled);
    }
}
