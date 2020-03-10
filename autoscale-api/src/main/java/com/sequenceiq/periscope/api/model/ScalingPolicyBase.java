package com.sequenceiq.periscope.api.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.periscope.api.endpoint.validator.ValidScalingPolicy;
import com.sequenceiq.periscope.doc.ApiDescription.ScalingPolicyJsonProperties;

import io.swagger.annotations.ApiModelProperty;

@ValidScalingPolicy
public class ScalingPolicyBase implements Json {

    @ApiModelProperty(ScalingPolicyJsonProperties.NAME)
    @Pattern(regexp = "(^[a-zA-Z][-a-zA-Z0-9]*$)",
            message = "The name can only contain alphanumeric characters and hyphens and has to start with an alphanumeric character")
    private String name;

    @ApiModelProperty(ScalingPolicyJsonProperties.ADJUSTMENTTYPE)
    @NotNull
    private AdjustmentType adjustmentType;

    @ApiModelProperty(ScalingPolicyJsonProperties.SCALINGADJUSTMENT)
    private int scalingAdjustment;

    @ApiModelProperty(ScalingPolicyJsonProperties.ALERTID)
    private long alertId;

    @ApiModelProperty(ScalingPolicyJsonProperties.HOSTGROUP)
    @NotEmpty
    @Size(max = 250)
    @Pattern(regexp = "(^[a-zA-Z][-a-zA-Z0-9]*$)",
            message = "The hostGroup can only contain alphanumeric characters and hyphens and has to start with an alphanumeric character")
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
