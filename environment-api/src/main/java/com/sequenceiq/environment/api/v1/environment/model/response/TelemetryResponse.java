package com.sequenceiq.environment.api.v1.environment.model.response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "TelemetryV1Response")
public class TelemetryResponse {

    private LoggingResponse logging;

    private WorkloadAnalyticsResponse workloadAnalytics;

    public LoggingResponse getLogging() {
        return logging;
    }

    public void setLogging(LoggingResponse logging) {
        this.logging = logging;
    }

    public WorkloadAnalyticsResponse getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(WorkloadAnalyticsResponse workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }
}
