package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.ScalingPolicyJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ScalingPolicyResponse extends ScalingPolicyBase {

    @ApiModelProperty(ScalingPolicyJsonProperties.ID)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
