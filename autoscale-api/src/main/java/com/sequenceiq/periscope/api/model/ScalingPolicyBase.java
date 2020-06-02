package com.sequenceiq.periscope.api.model;

import javax.validation.constraints.Pattern;

import com.sequenceiq.periscope.api.endpoint.validator.ValidScalingPolicy;
import com.sequenceiq.periscope.doc.ApiDescription.ScalingPolicyJsonProperties;

import io.swagger.annotations.ApiModelProperty;

@ValidScalingPolicy
public class ScalingPolicyBase implements Json {

    @ApiModelProperty(ScalingPolicyJsonProperties.NAME)
    @Pattern(regexp = "(^[a-zA-Z][-a-zA-Z0-9]*$)",
            message = "The name can only contain alphanumeric characters and hyphens and has to start with an alphabetic character")
    private String name;

    @ApiModelProperty(ScalingPolicyJsonProperties.ADJUSTMENTTYPE)
    private AdjustmentType adjustmentType;

    @ApiModelProperty(ScalingPolicyJsonProperties.SCALINGADJUSTMENT)
    private int scalingAdjustment;

    @ApiModelProperty(ScalingPolicyJsonProperties.ALERTID)
    private long alertId;

    @ApiModelProperty(ScalingPolicyJsonProperties.HOSTGROUP)
    private String hostGroup;

    public long getAlertId() {
        return alertId;
    }

    public void setAlertId(long alertId) {
        this.alertId = alertId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public int getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(int scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }
}
