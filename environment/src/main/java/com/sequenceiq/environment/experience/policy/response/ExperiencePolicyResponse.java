package com.sequenceiq.environment.experience.policy.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperiencePolicyResponse implements Serializable {

    @ApiModelProperty(value = "AWS credential prerequisite policy")
    private ProviderPolicyResponse aws;

    @ApiModelProperty(value = "AZURE credential prerequisite policy")
    private ProviderPolicyResponse azure;

    public ProviderPolicyResponse getAws() {
        return aws;
    }

    public void setAws(ProviderPolicyResponse aws) {
        this.aws = aws;
    }

    public ProviderPolicyResponse getAzure() {
        return azure;
    }

    public void setAzure(ProviderPolicyResponse azure) {
        this.azure = azure;
    }

}
