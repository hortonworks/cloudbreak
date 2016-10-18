package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityRuleResponse extends SecurityRuleBase {

    @ApiModelProperty(value = ModelDescriptions.ID)
    private Long id;

    public SecurityRuleResponse() {

    }

    public SecurityRuleResponse(String subnet) {
        super(subnet);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
