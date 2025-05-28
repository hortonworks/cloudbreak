package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpFreeIpaParameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AttachedFreeIpaRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachedFreeIpaRequest implements Serializable {

    @NotNull
    @Schema(description = EnvironmentModelDescription.CREATE_FREEIPA, required = true)
    private Boolean create;

    @Schema(description = EnvironmentModelDescription.FREEIPA_INSTANCE_COUNT_BY_GROUP)
    private Integer instanceCountByGroup;

    @Schema(description = EnvironmentModelDescription.FREEIPA_LOADBALANCER)
    private String loadBalancerType;

    @Schema(description = EnvironmentModelDescription.FREEIPA_INSTANCE_TYPE)
    private String instanceType;

    @Schema(description = EnvironmentModelDescription.MULTIAZ_FREEIPA)
    private boolean enableMultiAz;

    @Schema(description = EnvironmentModelDescription.CLOUD_PROVIDER_VARIANT)
    private String platformVariant;

    @Schema(description = EnvironmentModelDescription.FREEIPA_RECIPE_LIST)
    private Set<String> recipes;

    @Valid
    @Schema(description = EnvironmentModelDescription.FREEIPA_AWS_PARAMETERS)
    private AwsFreeIpaParameters aws;

    @Valid
    @Schema(description = EnvironmentModelDescription.FREEIPA_AZURE_PARAMETERS)
    private AzureFreeIpaParameters azure;

    @Valid
    @Schema(description = EnvironmentModelDescription.FREEIPA_GCP_PARAMETERS)
    private GcpFreeIpaParameters gcp;

    @Valid
    @Schema(description = EnvironmentModelDescription.FREEIPA_IMAGE)
    private FreeIpaImageRequest image;

    @Schema(description = EnvironmentModelDescription.FREEIPA_SECURITY)
    private FreeIpaSecurityRequest security;

    @Schema(description = EnvironmentModelDescription.FREEIPA_ARCHITECTURE)
    private String architecture;

    public Boolean getCreate() {
        return create;
    }

    public void setCreate(Boolean create) {
        this.create = create;
    }

    public Integer getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public void setInstanceCountByGroup(Integer instanceCountByGroup) {
        this.instanceCountByGroup = instanceCountByGroup;
    }

    public String getLoadBalancerType() {
        return loadBalancerType;
    }

    public void setLoadBalancerType(String loadBalancerType) {
        this.loadBalancerType = loadBalancerType;
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

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public FreeIpaSecurityRequest getSecurity() {
        return security;
    }

    public void setSecurity(FreeIpaSecurityRequest security) {
        this.security = security;
    }

    public AzureFreeIpaParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureFreeIpaParameters azure) {
        this.azure = azure;
    }

    public GcpFreeIpaParameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpFreeIpaParameters gcp) {
        this.gcp = gcp;
    }

    public FreeIpaImageRequest getImage() {
        return image;
    }

    public void setImage(FreeIpaImageRequest image) {
        this.image = image;
    }

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }

    public Set<String> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<String> recipes) {
        this.recipes = recipes;
    }

    @Override
    public String toString() {
        return "AttachedFreeIpaRequest{" +
                "create=" + create +
                ", instanceCountByGroup=" + instanceCountByGroup +
                ", loadBalancer=" + loadBalancerType +
                ", instanceType=" + instanceType +
                ", enableMultiAz=" + enableMultiAz +
                ", platformVariant=" + platformVariant +
                ", aws=" + aws +
                ", azure=" + azure +
                ", gcp=" + gcp +
                ", image=" + image +
                ", security=" + security +
                ", architecture=" + architecture +
                '}';
    }
}
