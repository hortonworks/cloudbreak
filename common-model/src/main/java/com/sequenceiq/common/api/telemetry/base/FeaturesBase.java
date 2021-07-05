package com.sequenceiq.common.api.telemetry.base;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;
import com.sequenceiq.common.api.type.FeatureSetting;

import io.swagger.annotations.ApiModelProperty;

public abstract class FeaturesBase implements Serializable {

    @JsonProperty("workloadAnalytics")
    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_WORKLOAD_ANALYTICS)
    private FeatureSetting workloadAnalytics;

    @JsonProperty("clusterLogsCollection")
    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_CLUSTER_LOGS_COLLECTION_ENABLED)
    private FeatureSetting clusterLogsCollection;

    @JsonProperty("monitoring")
    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_CLUSTER_MONITORING_ENABLED)
    private FeatureSetting monitoring;

    @JsonProperty("cloudStorageLogging")
    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_CLOUD_STORAGE_LOGGING_ENABLED)
    private FeatureSetting cloudStorageLogging;

    public FeatureSetting getWorkloadAnalytics() {
        return workloadAnalytics;
    }

    public void setWorkloadAnalytics(FeatureSetting workloadAnalytics) {
        this.workloadAnalytics = workloadAnalytics;
    }

    public FeatureSetting getClusterLogsCollection() {
        return clusterLogsCollection;
    }

    public void setClusterLogsCollection(FeatureSetting clusterLogsCollection) {
        this.clusterLogsCollection = clusterLogsCollection;
    }

    public FeatureSetting getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(FeatureSetting monitoring) {
        this.monitoring = monitoring;
    }

    public FeatureSetting getCloudStorageLogging() {
        return cloudStorageLogging;
    }

    public void setCloudStorageLogging(FeatureSetting cloudStorageLogging) {
        this.cloudStorageLogging = cloudStorageLogging;
    }

    @JsonIgnore
    public void addWorkloadAnalytics(boolean enabled) {
        workloadAnalytics = new FeatureSetting();
        workloadAnalytics.setEnabled(enabled);
    }

    @JsonIgnore
    public void addClusterLogsCollection(boolean enabled) {
        clusterLogsCollection = new FeatureSetting();
        clusterLogsCollection.setEnabled(enabled);
    }

    @JsonIgnore
    public void addMonitoring(boolean enabled) {
        monitoring = new FeatureSetting();
        monitoring.setEnabled(enabled);
    }

    @JsonIgnore
    public void addCloudStorageLogging(boolean enabled) {
        cloudStorageLogging = new FeatureSetting();
        cloudStorageLogging.setEnabled(enabled);
    }

    @Override
    public String toString() {
        return "FeaturesBase{" +
                "workloadAnalytics=" + workloadAnalytics +
                ", clusterLogsCollection=" + clusterLogsCollection +
                ", monitoring=" + monitoring +
                ", cloudStorageLogging=" + cloudStorageLogging +
                '}';
    }
}
