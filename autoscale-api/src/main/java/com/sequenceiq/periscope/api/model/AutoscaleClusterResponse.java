package com.sequenceiq.periscope.api.model;

import java.util.List;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AutoscaleClusterResponse extends ClusterBaseJson {

    @ApiModelProperty(ClusterJsonsProperties.STATE)
    private String state;

    @ApiModelProperty(ClusterJsonsProperties.AUTOSCALING_ENABLED)
    private boolean autoscalingEnabled;

    @ApiModelProperty(ClusterJsonsProperties.TIME_ALERTS)
    private List<TimeAlertResponse> timeAlerts;

    @ApiModelProperty(ClusterJsonsProperties.LOAD_ALERTS)
    private List<LoadAlertResponse> loadAlerts;

    @ApiModelProperty(ClusterJsonsProperties.SCALING_CONFIGURATION)
    private ScalingConfigurationRequest scalingConfiguration;

    public AutoscaleClusterResponse() {
    }

    public AutoscaleClusterResponse(String host, String port, String user, String stackCrn, boolean autoscalingEnabled, String state) {
        super(host, port, user, stackCrn);
        this.state = state;
        this.autoscalingEnabled = autoscalingEnabled;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public ScalingConfigurationRequest getScalingConfiguration() {
        return scalingConfiguration;
    }

    public void setScalingConfiguration(ScalingConfigurationRequest scalingConfiguration) {
        this.scalingConfiguration = scalingConfiguration;
    }
}
