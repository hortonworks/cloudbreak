package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidMultiSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxChildResourceMarkingRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @ApiModelProperty(ModelDescriptions.ENVIRONMENT_CRN)
    @NotNull
    private String parentCrn;

    @ValidMultiSecretType
    @NotEmpty
    @ApiModelProperty("Secret to be rotated")
    private String secret;

    public String getParentCrn() {
        return parentCrn;
    }

    public void setParentCrn(String parentCrn) {
        this.parentCrn = parentCrn;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
