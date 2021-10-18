package com.sequenceiq.distrox.api.v1.distrox.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupNetworkScaleModelDescription;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkScaleV1Request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistroXScaleV1Request implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String group;

    @NotNull
    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer desiredCount;

    @ApiModelProperty(value = InstanceGroupNetworkScaleModelDescription.NETWORK_SCALE_REQUEST)
    private NetworkScaleV1Request networkScaleRequest;

    private AdjustmentType adjustmentType = AdjustmentType.EXACT;

    private Long threshold;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Integer getDesiredCount() {
        return desiredCount;
    }

    public void setDesiredCount(Integer desiredCount) {
        this.desiredCount = desiredCount;
    }

    public NetworkScaleV1Request getNetworkScaleRequest() {
        return networkScaleRequest;
    }

    public void setNetworkScaleRequest(NetworkScaleV1Request networkScaleRequest) {
        this.networkScaleRequest = networkScaleRequest;
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
        return threshold;
    }
}
