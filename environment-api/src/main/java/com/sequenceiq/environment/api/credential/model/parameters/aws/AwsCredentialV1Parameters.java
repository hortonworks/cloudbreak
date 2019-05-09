package com.sequenceiq.environment.api.credential.model.parameters.aws;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsCredentialV1Parameters implements Serializable {

    @ApiModelProperty
    private KeyBasedCredentialParameters keyBased;

    @ApiModelProperty
    private RoleBasedCredentialParameters roleBased;

    @NotNull
    @ApiModelProperty(required = true)
    private Boolean govCloud = false;

    public KeyBasedCredentialParameters getKeyBased() {
        return keyBased;
    }

    public RoleBasedCredentialParameters getRoleBased() {
        return roleBased;
    }

    public void setKeyBased(KeyBasedCredentialParameters keyBased) {
        this.keyBased = keyBased;
    }

    public void setRoleBased(RoleBasedCredentialParameters roleBased) {
        this.roleBased = roleBased;
    }

    public boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }
}
