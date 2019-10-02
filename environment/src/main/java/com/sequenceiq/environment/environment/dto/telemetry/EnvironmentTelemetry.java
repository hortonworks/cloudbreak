package com.sequenceiq.environment.environment.dto.telemetry;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentTelemetry implements Serializable {

    private EnvironmentLogging logging;

    private EnvironmentWorkloadAnalytics workloadAnalytics;

    private boolean reportDeploymentLogs;

    private EnvironmentFeatures features;

    private Map<String, Object> fluentAttributes = new HashMap<>();

    public EnvironmentLogging getLogging() {
        return logging;
    }

    public void setLogging(EnvironmentLogging logging) {
        this.logging = logging;
    }

    public EnvironmentWorkloadAnalytics getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(EnvironmentWorkloadAnalytics workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }

    public boolean isReportDeploymentLogs() {
        return reportDeploymentLogs;
    }

    public void setReportDeploymentLogs(boolean reportDeploymentLogs) {
        this.reportDeploymentLogs = reportDeploymentLogs;
    }

    public EnvironmentFeatures getFeatures() {
        return features;
    }

    public void setFeatures(EnvironmentFeatures features) {
        this.features = features;
    }

    public Map<String, Object> getFluentAttributes() {
        return fluentAttributes;
    }

    public void setFluentAttributes(Map<String, Object> fluentAttributes) {
        this.fluentAttributes = fluentAttributes;
    }
}