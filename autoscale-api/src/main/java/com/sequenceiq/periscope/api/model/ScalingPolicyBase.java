package com.sequenceiq.periscope.api.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.periscope.api.endpoint.validator.ValidScalingPolicy;
import com.sequenceiq.periscope.doc.ApiDescription.ScalingPolicyJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@ValidScalingPolicy
public class ScalingPolicyBase implements Json {

    @Schema(description = ScalingPolicyJsonProperties.NAME)
    @Pattern(regexp = "(^[a-zA-Z][-a-zA-Z0-9]*$)",
            message = "The name can only contain alphanumeric characters and hyphens and has to start with an alphabetic character")
    private String name;

    @Schema(description = ScalingPolicyJsonProperties.ADJUSTMENTTYPE)
    @NotNull
    private AdjustmentType adjustmentType;

    @Schema(description = ScalingPolicyJsonProperties.SCALINGADJUSTMENT)
    private int scalingAdjustment;

    @Schema(description = ScalingPolicyJsonProperties.HOSTGROUP)
    @NotEmpty
    @Size(max = 250)
    @Pattern(regexp = "(^[a-zA-Z][-a-zA-Z0-9]*$)",
            message = "The hostGroup can only contain alphanumeric characters and hyphens and has to start with an alphabetic character")
    private String hostGroup;

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
