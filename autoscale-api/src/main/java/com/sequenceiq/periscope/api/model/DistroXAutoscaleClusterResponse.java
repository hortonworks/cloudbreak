package com.sequenceiq.periscope.api.model;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class DistroXAutoscaleClusterResponse implements Json {

    @Schema(description = ClusterJsonsProperties.STACK_NAME)
    private String stackName;

    @Schema(description = ClusterJsonsProperties.STACK_CRN)
    private String stackCrn;

    @Schema(description = ClusterJsonsProperties.STATE)
    private ClusterState state;

    @Schema(description = ClusterJsonsProperties.STACK_TYPE)
    private StackType stackType;

    @Schema(description = ClusterJsonsProperties.AUTOSCALING_ENABLED, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean autoscalingEnabled = Boolean.FALSE;

    @Schema(description = ClusterJsonsProperties.STOP_START_SCALING_ENABLED, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean stopStartScalingEnabled = Boolean.FALSE;

    @Schema(description = ClusterJsonsProperties.TIME_ALERTS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TimeAlertResponse> timeAlerts = new ArrayList<>();

    @Schema(description = ClusterJsonsProperties.LOAD_ALERTS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<LoadAlertResponse> loadAlerts = new ArrayList<>();

    public DistroXAutoscaleClusterResponse() {
    }

    public DistroXAutoscaleClusterResponse(String stackCrn, String stackName,
            boolean autoscalingEnabled, ClusterState state) {
        this.stackCrn = stackCrn;
        this.stackName = stackName;
        this.state = state;
        this.autoscalingEnabled = autoscalingEnabled;
    }

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }

    public StackType getStackType() {
        return stackType;
    }

    public void setStackType(StackType stackType) {
        this.stackType = stackType;
    }

    public boolean isAutoscalingEnabled() {
        return autoscalingEnabled;
    }

    public void setAutoscalingEnabled(boolean autoscalingEnabled) {
        this.autoscalingEnabled = autoscalingEnabled;
    }

    public List<TimeAlertResponse> getTimeAlerts() {
        return timeAlerts;
    }

    public void setTimeAlerts(List<TimeAlertResponse> timeAlerts) {
        this.timeAlerts = timeAlerts;
    }

    public List<LoadAlertResponse> getLoadAlerts() {
        return loadAlerts;
    }

    public void setLoadAlerts(List<LoadAlertResponse> loadAlerts) {
        this.loadAlerts = loadAlerts;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }

    public Boolean isStopStartScalingEnabled() {
        return stopStartScalingEnabled;
    }

    public void setStopStartScalingEnabled(Boolean stopStartScalingEnabled) {
        this.stopStartScalingEnabled = stopStartScalingEnabled;
    }
}
