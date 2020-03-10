package com.sequenceiq.periscope.api.model;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;
import com.sequenceiq.periscope.doc.ApiDescription.DistroXClusterJsonsProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DistroXAutoscaleClusterRequest implements Json {
    @ApiModelProperty(ClusterJsonsProperties.ENABLE_AUTOSCALING)
    private @NotNull Boolean enableAutoscaling;

    @ApiModelProperty(DistroXClusterJsonsProperties.DISTROX_SCALING_MODE)
    private @Valid AutoscalingModeType autoScalingMode;

    @ApiModelProperty(ClusterJsonsProperties.SCALING_CONFIGURATION)
    private @Valid ScalingConfigurationRequest scalingConfiguration;

    @ApiModelProperty(ClusterJsonsProperties.TIME_ALERTS)
    private @Valid List<TimeAlertRequest> timeAlertRequests;

    @ApiModelProperty(ClusterJsonsProperties.LOAD_ALERTS)
    private @Valid List<LoadAlertRequest> loadAlertRequests;

    public DistroXAutoscaleClusterRequest() {
    }

    public DistroXAutoscaleClusterRequest(boolean enableAutoscaling) {
        this.enableAutoscaling = enableAutoscaling;
    }

    public void setEnableAutoscaling(Boolean enableAutoscaling) {
        this.enableAutoscaling = enableAutoscaling;
    }

    public List<TimeAlertRequest> getTimeAlertRequests() {
        return timeAlertRequests;
    }

    public void setTimeAlertRequests(List<TimeAlertRequest> timeAlertRequests) {
        this.timeAlertRequests = timeAlertRequests;
    }

    public ScalingConfigurationRequest getScalingConfiguration() {
        return scalingConfiguration;
    }

    public void setScalingConfiguration(ScalingConfigurationRequest scalingConfiguration) {
        this.scalingConfiguration = scalingConfiguration;
    }

    public Boolean getEnableAutoscaling() {
        return enableAutoscaling;
    }

    public List<LoadAlertRequest> getLoadAlertRequests() {
        return loadAlertRequests;
    }

    public void setLoadAlertRequests(List<LoadAlertRequest> loadAlertRequests) {
        this.loadAlertRequests = loadAlertRequests;
    }

    public AutoscalingModeType getAutoScalingMode() {
        return autoScalingMode;
    }

    public void setAutoScalingMode(AutoscalingModeType autoScalingMode) {
        this.autoScalingMode = autoScalingMode;
    }
}
