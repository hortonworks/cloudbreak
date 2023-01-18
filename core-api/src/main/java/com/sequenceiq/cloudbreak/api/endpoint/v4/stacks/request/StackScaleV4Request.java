package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupNetworkScaleModelDescription;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackScaleV4Request implements JsonEntity {

    @NotNull
    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String group;

    @NotNull(message = "desiredCount must not be null.")
    @Schema(description = InstanceGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer desiredCount;

    private Long stackId;

    @Schema(description = InstanceGroupAdjustmentModelDescription.FORCE)
    private Boolean forced;

    @Schema(description = InstanceGroupAdjustmentModelDescription.ADJUSTMENT_TYPE)
    private AdjustmentType adjustmentType = AdjustmentType.EXACT;

    @Schema(description = InstanceGroupAdjustmentModelDescription.THRESHOLD)
    private Long threshold;

    @Schema(description = InstanceGroupNetworkScaleModelDescription.NETWORK_SCALE_REQUEST)
    private NetworkScaleV4Request networkScaleV4Request;

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

    @JsonIgnore
    public Long getStackId() {
        return stackId;
    }

    @JsonIgnore
    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public Boolean getForced() {
        return forced;
    }

    public void setForced(Boolean forced) {
        this.forced = forced;
    }

    public NetworkScaleV4Request getStackNetworkScaleV4Request() {
        return networkScaleV4Request;
    }

    public void setStackNetworkScaleV4Request(NetworkScaleV4Request networkScaleV4Request) {
        this.networkScaleV4Request = networkScaleV4Request;
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
