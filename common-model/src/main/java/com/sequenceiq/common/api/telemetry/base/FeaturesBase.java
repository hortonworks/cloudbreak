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
}
