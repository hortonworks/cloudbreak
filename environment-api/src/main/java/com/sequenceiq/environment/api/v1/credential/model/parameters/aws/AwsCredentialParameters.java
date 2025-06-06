package com.sequenceiq.environment.api.v1.credential.model.parameters.aws;

import java.io.Serializable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AwsCredentialV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsCredentialParameters implements Serializable {

    @Valid
    @Schema
    private KeyBasedParameters keyBased;

    @Valid
    @Schema
    private RoleBasedParameters roleBased;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean govCloud = false;

    private String defaultRegion;

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

    public Boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    @Override
    public String toString() {
        return "AwsCredentialParameters{" +
                "keyBased=" + keyBased +
                ", roleBased=" + roleBased +
                ", govCloud=" + govCloud +
                ", defaultRegion=" + defaultRegion +
                '}';
    }
}
