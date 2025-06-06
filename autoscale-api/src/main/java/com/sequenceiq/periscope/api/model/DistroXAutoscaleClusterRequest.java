package com.sequenceiq.periscope.api.model;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.periscope.api.endpoint.validator.ValidDistroXAutoscaleRequest;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@ValidDistroXAutoscaleRequest
@Schema
public class DistroXAutoscaleClusterRequest implements Json {
    @Schema(description = ClusterJsonsProperties.ENABLE_AUTOSCALING)
    private @NotNull Boolean enableAutoscaling;

    @Schema(description = ClusterJsonsProperties.ENABLE_STOP_START_SCALING)
    private Boolean useStopStartMechanism;

    @Schema(description = ClusterJsonsProperties.TIME_ALERTS)
    private List<@Valid TimeAlertRequest> timeAlertRequests;

    @Schema(description = ClusterJsonsProperties.LOAD_ALERTS)
    private List<@Valid LoadAlertRequest> loadAlertRequests;

    public DistroXAutoscaleClusterRequest() {
    }

    public List<TimeAlertRequest> getTimeAlertRequests() {
        return timeAlertRequests != null ? timeAlertRequests : List.of();
    }

    public void setTimeAlertRequests(List<TimeAlertRequest> timeAlertRequests) {
        this.timeAlertRequests = timeAlertRequests;
    }

    public Boolean getEnableAutoscaling() {
        return enableAutoscaling;
    }

    public void setEnableAutoscaling(Boolean enableAutoscaling) {
        this.enableAutoscaling = enableAutoscaling;
    }

    public List<LoadAlertRequest> getLoadAlertRequests() {
        return loadAlertRequests != null ? loadAlertRequests : List.of();
    }

    public void setLoadAlertRequests(List<LoadAlertRequest> loadAlertRequests) {
        this.loadAlertRequests = loadAlertRequests;
    }

    public Boolean getUseStopStartMechanism() {
        return useStopStartMechanism;
    }

    public void setUseStopStartMechanism(Boolean useStopStartMechanism) {
        this.useStopStartMechanism = useStopStartMechanism;
    }
}
