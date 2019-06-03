package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.logging.LoggingV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.workload.WorkloadAnalyticsV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class TelemetryV4Request implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_LOGGING)
    private LoggingV4Request logging;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_WA)
    private WorkloadAnalyticsV4Request workloadAnalytics;

    public LoggingV4Request getLogging() {
        return logging;
    }

    public void setLogging(LoggingV4Request logging) {
        this.logging = logging;
    }

    public WorkloadAnalyticsV4Request getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(WorkloadAnalyticsV4Request workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }
}
