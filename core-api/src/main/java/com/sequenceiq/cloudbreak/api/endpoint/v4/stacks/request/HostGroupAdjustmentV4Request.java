package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupAdjustmentModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupAdjustmentV4Request implements JsonEntity {

    @NotNull
    @Schema(description = HostGroupModelDescription.HOST_GROUP_NAME, required = true)
    private String hostGroup;

    @NotNull
    @Schema(description = HostGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    private Integer scalingAdjustment;

    @Schema(description = HostGroupAdjustmentModelDescription.WITH_STACK_UPDATE)
    private Boolean withStackUpdate = Boolean.FALSE;

    @Schema(description = HostGroupAdjustmentModelDescription.VALIDATE_NODE_COUNT)
    private Boolean validateNodeCount = Boolean.TRUE;

    @Schema(description = HostGroupAdjustmentModelDescription.FORCED)
    private Boolean forced;

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    public Boolean getWithStackUpdate() {
        return withStackUpdate;
    }

    public void setWithStackUpdate(Boolean withStackUpdate) {
        this.withStackUpdate = withStackUpdate;
    }

    public Boolean getValidateNodeCount() {
        return validateNodeCount;
    }

    public void setValidateNodeCount(Boolean validateNodeCount) {
        this.validateNodeCount = validateNodeCount;
    }

    public void setForced(Boolean forced) {
        this.forced = forced;
    }

    public Boolean getForced() {
        return forced;
    }
}
