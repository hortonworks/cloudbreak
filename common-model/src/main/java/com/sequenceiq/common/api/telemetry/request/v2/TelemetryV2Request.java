package com.sequenceiq.common.api.telemetry.request.v2;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.telemetry.base.TelemetryBase;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.MonitoringRequest;
import com.sequenceiq.common.api.telemetry.request.WorkloadAnalyticsRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("TelemetryV2Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryV2Request extends TelemetryBase {

    @Valid
    @NotNull
    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_LOGGING)
    private LoggingRequest logging;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_MONITORING)
    private MonitoringRequest monitoring;

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

    public MonitoringRequest getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringRequest monitoring) {
        this.monitoring = monitoring;
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

    @Override
    public String toString() {
        return "TelemetryV2Request{" +
                "logging=" + logging +
                ", monitoring=" + monitoring +
                ", workloadAnalytics=" + workloadAnalytics +
                ", features=" + features +
                "} " + super.toString();
    }

}
