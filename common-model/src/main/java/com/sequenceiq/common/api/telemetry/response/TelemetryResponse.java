package com.sequenceiq.common.api.telemetry.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.telemetry.base.TelemetryBase;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "TelemetryResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryResponse extends TelemetryBase {

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_LOGGING)
    private LoggingResponse logging;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_MONITORING)
    private MonitoringResponse monitoring;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_WORKLOAD_ANALYTICS)
    private WorkloadAnalyticsResponse workloadAnalytics;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_FEATURES)
    private FeaturesResponse features;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_RULES)
    private List<AnonymizationRule> rules;

    public LoggingResponse getLogging() {
        return logging;
    }

    public void setLogging(LoggingResponse logging) {
        this.logging = logging;
    }

    public MonitoringResponse getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringResponse monitoring) {
        this.monitoring = monitoring;
    }

    public WorkloadAnalyticsResponse getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(WorkloadAnalyticsResponse workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }

    public FeaturesResponse getFeatures() {
        return features;
    }

    public void setFeatures(FeaturesResponse features) {
        this.features = features;
    }

    public List<AnonymizationRule> getRules() {
        return rules;
    }

    public void setRules(List<AnonymizationRule> rules) {
        this.rules = rules;
    }

    @Override
    public String toString() {
        return "TelemetryResponse{" +
                "logging=" + logging +
                ", monitoring=" + monitoring +
                ", workloadAnalytics=" + workloadAnalytics +
                ", features=" + features +
                ", rules=" + rules +
                "} " + super.toString();
    }
}
