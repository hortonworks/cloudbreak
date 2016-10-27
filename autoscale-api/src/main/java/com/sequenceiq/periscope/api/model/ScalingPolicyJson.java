package com.sequenceiq.periscope.api.model;

import javax.validation.constraints.Pattern;

import com.sequenceiq.periscope.doc.ApiDescription.ScalingPolicyJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ScalingPolicy")
public class ScalingPolicyJson implements Json {

    @ApiModelProperty(ScalingPolicyJsonProperties.ID)
    private Long id;
    @ApiModelProperty(ScalingPolicyJsonProperties.NAME)
    @Pattern(regexp = "([a-zA-Z][-a-zA-Z0-9]*)",
            message = "The name can only contain alphanumeric characters and hyphens and has start with an alphanumeric character")
    private String name;
    @ApiModelProperty(ScalingPolicyJsonProperties.ADJUSTMENTTYPE)
    private AdjustmentType adjustmentType;
    @ApiModelProperty(ScalingPolicyJsonProperties.SCALINGADJUSTMENT)
    private int scalingAdjustment;
    @ApiModelProperty(ScalingPolicyJsonProperties.ALERTID)
    private long alertId;
    @ApiModelProperty(ScalingPolicyJsonProperties.HOSTGROUP)
    private String hostGroup;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
