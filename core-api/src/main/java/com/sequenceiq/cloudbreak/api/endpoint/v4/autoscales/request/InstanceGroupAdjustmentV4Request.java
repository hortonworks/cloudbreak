package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class InstanceGroupAdjustmentV4Request implements JsonEntity {

    public static final long HUNDRED_PERCENT = 100L;

    @NotNull
    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String instanceGroup;

    @NotNull
    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer scalingAdjustment;

    @ApiModelProperty(value = ModelDescriptions.InstanceGroupNetworkScaleModelDescription.NETWORK_SCALE_REQUEST)
    private NetworkScaleV4Request networkScaleRequest;

    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.ADJUSTMENT_TYPE)
    private AdjustmentType adjustmentType = AdjustmentType.EXACT;

    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.THRESHOLD)
    private Long threshold;

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    public NetworkScaleV4Request getNetworkScaleRequest() {
        return networkScaleRequest;
    }

    public void setNetworkScaleRequest(NetworkScaleV4Request networkScaleV4Request) {
        this.networkScaleRequest = networkScaleV4Request;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setThreshold(Long threshold) {
        this.threshold = threshold;
    }

    public Long getThreshold() {
        if (threshold != null) {
            return threshold;
        } else if (AdjustmentType.PERCENTAGE.equals(adjustmentType)) {
            return HUNDRED_PERCENT;
        } else if (scalingAdjustment != null) {
            return Long.valueOf(scalingAdjustment);
        } else {
            return null;
        }
    }

}
