package com.sequenceiq.environment.api.v1.credential.model.parameters.aws;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AwsCredentialV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsCredentialParameters implements Serializable {

    @ApiModelProperty
    private KeyBasedParameters keyBased;

    @ApiModelProperty
    private RoleBasedParameters roleBased;

    @NotNull
    @ApiModelProperty(required = true)
    private Boolean govCloud = false;

    public KeyBasedParameters getKeyBased() {
        return keyBased;
    }

    public RoleBasedParameters getRoleBased() {
        return roleBased;
    }

    public void setKeyBased(KeyBasedParameters keyBased) {
        this.keyBased = keyBased;
    }

    public void setRoleBased(RoleBasedParameters roleBased) {
        this.roleBased = roleBased;
    }

    public boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }
}
