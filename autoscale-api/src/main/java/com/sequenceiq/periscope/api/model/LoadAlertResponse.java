package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.LoadAlertJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class LoadAlertResponse extends AbstractAlertJson {

    @Schema(description = BaseAlertJsonProperties.CRN)
    private String crn;

    @Schema(description = BaseAlertJsonProperties.SCALINGPOLICYID)
    private ScalingPolicyResponse scalingPolicy;

    @Schema(description = LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION)
    private LoadAlertConfigurationResponse loadAlertConfiguration;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public ScalingPolicyResponse getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicyResponse scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }

    public LoadAlertConfigurationResponse getLoadAlertConfiguration() {
        return loadAlertConfiguration;
    }

    public void setLoadAlertConfiguration(LoadAlertConfigurationResponse loadAlertConfiguration) {
        this.loadAlertConfiguration = loadAlertConfiguration;
    }
}
