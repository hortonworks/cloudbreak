package com.sequenceiq.common.api.telemetry.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.telemetry.base.TelemetryBase;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "TelemetryRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryRequest extends TelemetryBase {

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_LOGGING)
    private LoggingRequest logging;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_WORKLOAD_ANALYTICS)
    private WorkloadAnalyticsRequest workloadAnalytics;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_FEATURES)
    private FeaturesRequest features;

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

    public FeaturesRequest getFeatures() {
        return features;
    }

    public void setFeatures(FeaturesRequest features) {
        this.features = features;
    }
}
