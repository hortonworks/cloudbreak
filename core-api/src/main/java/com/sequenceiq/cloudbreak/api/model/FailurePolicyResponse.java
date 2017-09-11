package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("FailurePolicyResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FailurePolicyResponse extends FailurePolicyBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
