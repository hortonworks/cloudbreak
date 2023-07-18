package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.annotation.OnlyMultiSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxMultiSecretRotationRequest {

    @ValidCrn(resource = { CrnResourceDescriptor.DATALAKE, CrnResourceDescriptor.DATAHUB })
    @ApiModelProperty(ModelDescriptions.DATA_LAKE_CRN)
    private String crn;

    @OnlyMultiSecretType
    @NotEmpty
    @ApiModelProperty("Secret to be rotated")
    private String secret;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
