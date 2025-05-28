package com.sequenceiq.environment.api.v1.environment.model.response;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaResponse implements Serializable {

    @Schema(description = EnvironmentModelDescription.FREEIPA_INSTANCE_COUNT_BY_GROUP, requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer instanceCountByGroup = 1;

    @Schema(description = EnvironmentModelDescription.FREEIPA_INSTANCE_TYPE)
    private String instanceType;

    @Schema(description = EnvironmentModelDescription.FREEIPA_AWS_PARAMETERS)
    private AwsFreeIpaParameters aws;

    @Schema(description = EnvironmentModelDescription.FREEIPA_IMAGE)
    private FreeIpaImageResponse image;

    @Schema(description = EnvironmentModelDescription.MULTIAZ_FREEIPA, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean enableMultiAz;

    @Schema(description = EnvironmentModelDescription.FREEIPA_RECIPES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> recipes = new HashSet<>();

    @Schema(description = EnvironmentModelDescription.FREEIPA_SECURITY)
    private FreeIpaSecurityResponse security;

    @Schema(description = EnvironmentModelDescription.FREEIPA_ARCHITECTURE)
    private String architecture;

    public Integer getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public void setInstanceCountByGroup(Integer instanceCountByGroup) {
        this.instanceCountByGroup = instanceCountByGroup;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public AwsFreeIpaParameters getAws() {
        return aws;
    }

    public void setAws(AwsFreeIpaParameters aws) {
        this.aws = aws;
    }

    public FreeIpaImageResponse getImage() {
        return image;
    }

    public void setImage(FreeIpaImageResponse image) {
        this.image = image;
    }

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    public Set<String> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<String> recipes) {
        this.recipes = recipes;
    }

    public FreeIpaSecurityResponse getSecurity() {
        return security;
    }

    public void setSecurity(FreeIpaSecurityResponse security) {
        this.security = security;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    @Override
    public String toString() {
        return "FreeIpaResponse{" +
                "instanceCountByGroup=" + instanceCountByGroup +
                "instanceType=" + instanceType +
                ", aws=" + aws +
                ", image=" + image +
                ", enableMultiAz=" + enableMultiAz +
                ", recipes=" + recipes +
                ", security=" + security +
                '}';
    }
}
