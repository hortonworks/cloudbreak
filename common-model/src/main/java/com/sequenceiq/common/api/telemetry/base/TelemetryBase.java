package com.sequenceiq.common.api.telemetry.base;

import java.io.Serializable;

import com.sequenceiq.common.api.telemetry.common.TelemetrySetting;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class TelemetryBase implements Serializable {

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_REPORT_DEPLOYMENT_LOGS_ENABLED)
    private TelemetrySetting reportDeploymentLogs = TelemetrySetting.ENABLED;

    public TelemetrySetting getReportDeploymentLogs() {
        return reportDeploymentLogs;
    }

    public void setReportDeploymentLogs(TelemetrySetting reportDeploymentLogs) {
        this.reportDeploymentLogs = reportDeploymentLogs;
    }
}
