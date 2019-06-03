package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.telemetry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.telemetry.logging.LoggingV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.telemetry.workload.WorkloadAnalyticsV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryV4Response implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_LOGGING)
    private LoggingV4Response logging;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TELEMETRY_WA)
    private WorkloadAnalyticsV4Response workloadAnalytics;

    public LoggingV4Response getLogging() {
        return logging;
    }

    public void setLogging(LoggingV4Response logging) {
        this.logging = logging;
    }

    public WorkloadAnalyticsV4Response getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(WorkloadAnalyticsV4Response workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }
}
