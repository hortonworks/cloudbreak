package com.sequenceiq.periscope.api.model;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class AutoscaleClusterRequest extends ClusterBaseJson {

    @Schema(description = ClusterJsonsProperties.WORKSPACE_ID)
    @NotNull
    @Min(1L)
    private Long workspaceId;

    @Schema(description = ClusterJsonsProperties.PASSWORD)
    private String pass;

    @Schema(description = ClusterJsonsProperties.ENABLE_AUTOSCALING)
    private boolean enableAutoscaling;

    @Valid
    @Schema(description = ClusterJsonsProperties.TIME_ALERTS)
    private List<TimeAlertRequest> timeAlerts;

    @Schema(description = ClusterJsonsProperties.SCALING_CONFIGURATION)
    @Valid
    private ScalingConfigurationRequest scalingConfiguration;

    public AutoscaleClusterRequest() {
    }

    public AutoscaleClusterRequest(String host, String port, String user, String pass, String stackCrn, boolean enableAutoscaling) {
        super(host, port, user, stackCrn);
        this.pass = pass;
        this.enableAutoscaling = enableAutoscaling;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public boolean enableAutoscaling() {
        return enableAutoscaling;
    }

    public void setEnableAutoscaling(boolean enableAutoscaling) {
        this.enableAutoscaling = enableAutoscaling;
    }

    public List<TimeAlertRequest> getTimeAlerts() {
        return timeAlerts;
    }

    public void setTimeAlerts(List<TimeAlertRequest> timeAlerts) {
        this.timeAlerts = timeAlerts;
    }

    public ScalingConfigurationRequest getScalingConfiguration() {
        return scalingConfiguration;
    }

    public void setScalingConfiguration(ScalingConfigurationRequest scalingConfiguration) {
        this.scalingConfiguration = scalingConfiguration;
    }
}
