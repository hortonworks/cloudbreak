package com.sequenceiq.periscope.api.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties;
import com.sequenceiq.periscope.doc.ApiDescription.LoadAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LoadAlertRequest extends AbstractAlertJson {

    @ApiModelProperty(LoadAlertJsonProperties.LOAD_ALERT_CONFIGURATION)
    private @Valid @NotNull LoadAlertConfigurationRequest loadAlertConfiguration;

    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
    private @Valid @NotNull ScalingPolicyRequest scalingPolicy;

    public ScalingPolicyRequest getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicyRequest scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }

    public LoadAlertConfigurationRequest getLoadAlertConfiguration() {
        return loadAlertConfiguration;
    }

    public void setLoadAlertConfiguration(LoadAlertConfigurationRequest loadAlertConfiguration) {
        this.loadAlertConfiguration = loadAlertConfiguration;
    }
}
