package com.sequenceiq.environment.api.v1.environment.model.request;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "TelemetryV1Request")
public class TelemetryRequest {

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY_LOGGING)
    private LoggingRequest logging;

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY_WORKLOAD_ANALYTICS)
    private WorkloadAnalyticsRequest workloadAnalytics;

    public LoggingRequest getLogging() {
        return logging;
    }

    public void setLogging(LoggingRequest logging) {
        this.logging = logging;
    }

    public WorkloadAnalyticsRequest getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(WorkloadAnalyticsRequest workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }
}
