package com.sequenceiq.environment.environment.dto.telemetry;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.telemetry.common.TelemetrySetting;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentTelemetry implements Serializable {

    private final EnvironmentLogging logging;

    private final EnvironmentWorkloadAnalytics workloadAnalytics;

    private final TelemetrySetting reportDeploymentLogs;

    public EnvironmentTelemetry(@JsonProperty("logging") EnvironmentLogging logging,
            @JsonProperty("workloadAnalytics") EnvironmentWorkloadAnalytics workloadAnalytics,
            @JsonProperty("reportDeploymentLogs") TelemetrySetting reportDeploymentLogs) {
        this.logging = logging;
        this.workloadAnalytics = workloadAnalytics;
        this.reportDeploymentLogs = reportDeploymentLogs;
    }

    public EnvironmentLogging getLogging() {
        return logging;
    }

    public EnvironmentWorkloadAnalytics getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public TelemetrySetting getReportDeploymentLogs() {
        return reportDeploymentLogs;
    }
}