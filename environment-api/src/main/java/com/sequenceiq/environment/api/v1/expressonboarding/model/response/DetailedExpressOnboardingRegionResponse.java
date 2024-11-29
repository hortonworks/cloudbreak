package com.sequenceiq.environment.api.v1.expressonboarding.model.response;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.platformresource.model.ExpressOnboardingCloudProvidersResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailedExpressOnboardingRegionResponse implements Serializable {

    @Schema(description = "")
    private DeploymentInformationResponse deploymentInformation;

    @Schema(description = "")
    private TenantInformationResponse tenantInformation;

    @Schema(description = "", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, ExpressOnboardingCloudProvidersResponse> cloudProviders = new HashMap<>();

    public DeploymentInformationResponse getDeploymentInformation() {
        return deploymentInformation;
    }

    public void setDeploymentInformation(DeploymentInformationResponse deploymentInformation) {
        this.deploymentInformation = deploymentInformation;
    }

    public TenantInformationResponse getTenantInformation() {
        return tenantInformation;
    }

    public void setTenantInformation(TenantInformationResponse tenantInformation) {
        this.tenantInformation = tenantInformation;
    }

    public Map<String, ExpressOnboardingCloudProvidersResponse> getCloudProviders() {
        return cloudProviders;
    }

    public void setCloudProviders(Map<String, ExpressOnboardingCloudProvidersResponse> cloudProviders) {
        this.cloudProviders = cloudProviders;
    }

    @Override
    public String toString() {
        return "DetailedExpressOnboardingResponse{" +
                "deploymentInformation=" + deploymentInformation +
                ", tenantInformation=" + tenantInformation +
                ", cloudProviders=" + cloudProviders +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DetailedExpressOnboardingRegionResponse that = (DetailedExpressOnboardingRegionResponse) o;
        return Objects.equals(deploymentInformation, that.deploymentInformation)
                && Objects.equals(tenantInformation, that.tenantInformation)
                && Objects.equals(cloudProviders, that.cloudProviders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deploymentInformation, tenantInformation, cloudProviders);
    }
}
