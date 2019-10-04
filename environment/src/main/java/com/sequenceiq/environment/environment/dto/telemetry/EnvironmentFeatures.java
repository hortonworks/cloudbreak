package com.sequenceiq.environment.environment.dto.telemetry;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.type.FeatureSetting;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentFeatures implements Serializable {

    private FeatureSetting workloadAnalytics;

    private FeatureSetting reportDeploymentLogs;

    public FeatureSetting getReportDeploymentLogs() {
        return reportDeploymentLogs;
    }

    public void setReportDeploymentLogs(FeatureSetting reportDeploymentLogs) {
        this.reportDeploymentLogs = reportDeploymentLogs;
    }

    public FeatureSetting getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(FeatureSetting workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }
}
