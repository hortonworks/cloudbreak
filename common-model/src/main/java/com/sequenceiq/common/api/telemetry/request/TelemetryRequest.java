package com.sequenceiq.common.api.telemetry.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.telemetry.base.TelemetryBase;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TelemetryRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryRequest extends TelemetryBase {

    @Schema(description = TelemetryModelDescription.TELEMETRY_LOGGING)
    private LoggingRequest logging;

    @Schema(description = TelemetryModelDescription.TELEMETRY_MONITORING)
    private MonitoringRequest monitoring;

    @Schema(description = TelemetryModelDescription.TELEMETRY_WORKLOAD_ANALYTICS)
    private WorkloadAnalyticsRequest workloadAnalytics;

    @Schema(description = TelemetryModelDescription.TELEMETRY_FEATURES)
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
        return "TelemetryRequest{" +
                "logging=" + logging +
                ", monitoring=" + monitoring +
                ", workloadAnalytics=" + workloadAnalytics +
                ", features=" + features +
                "} " + super.toString();
    }
}
