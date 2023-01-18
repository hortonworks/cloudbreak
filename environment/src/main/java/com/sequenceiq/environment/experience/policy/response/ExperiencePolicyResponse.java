package com.sequenceiq.environment.experience.policy.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperiencePolicyResponse implements Serializable {

    @Schema(description = "AWS credential prerequisite policy")
    private ProviderPolicyResponse aws;

    @Schema(description = "AZURE credential prerequisite policy")
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
