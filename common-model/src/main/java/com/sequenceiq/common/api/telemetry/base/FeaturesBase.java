package com.sequenceiq.common.api.telemetry.base;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;
import com.sequenceiq.common.api.type.FeatureSetting;

import io.swagger.annotations.ApiModelProperty;

public abstract class FeaturesBase implements Serializable {

    @JsonProperty("workloadAnalytics")
    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_WORKLOAD_ANALYTICS)
    private FeatureSetting workloadAnalytics;

    @JsonProperty("reportDeploymentLogs")
    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_REPORT_DEPLOYMENT_LOGS_ENABLED)
    private FeatureSetting reportDeploymentLogs;

    @JsonProperty("useSharedAltusCredential")
    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_USE_SHARED_ALTUS_CREDENTIAL_ENABLED)
    private FeatureSetting useSharedAltusCredential;

    public FeatureSetting getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(FeatureSetting workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }

    public FeatureSetting getReportDeploymentLogs() {
        return reportDeploymentLogs;
    }

    public void setReportDeploymentLogs(FeatureSetting reportDeploymentLogs) {
        this.reportDeploymentLogs = reportDeploymentLogs;
    }

    public FeatureSetting getUseSharedAltusCredential() {
        return useSharedAltusCredential;
    }

    public void setUseSharedAltusCredential(FeatureSetting useSharedAltusCredential) {
        this.useSharedAltusCredential = useSharedAltusCredential;
    }
}
