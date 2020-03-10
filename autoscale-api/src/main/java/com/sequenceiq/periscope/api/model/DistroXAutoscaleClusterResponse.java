package com.sequenceiq.periscope.api.model;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DistroXAutoscaleClusterResponse implements Json {
    @ApiModelProperty(ClusterJsonsProperties.ID)
    private long id;

    @ApiModelProperty(ClusterJsonsProperties.STACK_NAME)
    private String stackName;

    @ApiModelProperty(ClusterJsonsProperties.STACK_CRN)
    private String stackCrn;

    @ApiModelProperty(ClusterJsonsProperties.STATE)
    private ClusterState state;

    @ApiModelProperty(ClusterJsonsProperties.STACK_TYPE)
    private StackType stackType;

    @ApiModelProperty(ClusterJsonsProperties.AUTOSCALING_ENABLED)
    private Boolean autoscalingEnabled;

    @ApiModelProperty(ClusterJsonsProperties.TIME_ALERTS)
    private List<TimeAlertResponse> timeAlerts;

    @ApiModelProperty(ClusterJsonsProperties.LOAD_ALERTS)
    private List<LoadAlertResponse> loadAlerts;

    public DistroXAutoscaleClusterResponse() {
    }

    public DistroXAutoscaleClusterResponse(String stackCrn, String stackName,
            boolean autoscalingEnabled, long id, ClusterState state) {
        this.stackCrn = stackCrn;
        this.stackName = stackName;
        this.id = id;
        this.state = state;
        this.autoscalingEnabled = autoscalingEnabled;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
}
