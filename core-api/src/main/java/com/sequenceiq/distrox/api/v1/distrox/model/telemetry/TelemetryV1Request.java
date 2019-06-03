package com.sequenceiq.distrox.api.v1.distrox.model.telemetry;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.distrox.api.v1.distrox.model.telemetry.logging.LoggingV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.telemetry.workload.WorkloadAnalyticsV1Request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class TelemetryV1Request implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_LOGGING)
    private LoggingV1Request logging;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_WA)
    private WorkloadAnalyticsV1Request workloadAnalytics;

    public LoggingV1Request getLogging() {
        return logging;
    }

    public void setLogging(LoggingV1Request logging) {
        this.logging = logging;
    }

    public WorkloadAnalyticsV1Request getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(WorkloadAnalyticsV1Request workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }
}
