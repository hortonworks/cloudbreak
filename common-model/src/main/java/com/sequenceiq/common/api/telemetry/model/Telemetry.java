package com.sequenceiq.common.api.telemetry.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Telemetry implements Serializable {

    @JsonProperty("logging")
    private Logging logging;

    @JsonProperty("workloadAnalytics")
    private WorkloadAnalytics workloadAnalytics;

    @JsonProperty("databusEndpoint")
    private String databusEndpoint;

    @JsonProperty("meteringEnabled")
    private boolean meteringEnabled;

    @JsonProperty("reportDeploymentLogs")
    private boolean reportDeploymentLogs;

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

    public boolean isMeteringEnabled() {
        return meteringEnabled;
    }

    public void setMeteringEnabled(boolean meteringEnabled) {
        this.meteringEnabled = meteringEnabled;
    }

    public boolean isReportDeploymentLogs() {
        return reportDeploymentLogs;
    }

    public void setReportDeploymentLogs(boolean reportDeploymentLogs) {
        this.reportDeploymentLogs = reportDeploymentLogs;
    }
}
