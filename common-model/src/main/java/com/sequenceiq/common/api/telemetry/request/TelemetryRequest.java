package com.sequenceiq.common.api.telemetry.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "TelemetryRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryRequest implements Serializable {

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_LOGGING)
    private LoggingRequest logging;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_WORKLOAD_ANALYTICS)
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
