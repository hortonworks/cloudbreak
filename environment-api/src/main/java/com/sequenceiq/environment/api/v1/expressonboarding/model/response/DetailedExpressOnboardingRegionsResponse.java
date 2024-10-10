package com.sequenceiq.environment.api.v1.expressonboarding.model.response;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.platformresource.model.ExpressOnboardingCloudProvidersResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailedExpressOnboardingRegionsResponse {

    @Schema(description = "Regions by cloud providers")
    private Map<String, ExpressOnboardingCloudProvidersResponse> cloudProviders = new HashMap<>();

    public Map<String, ExpressOnboardingCloudProvidersResponse> getCloudProviders() {
        return cloudProviders;
    }

    public void setCloudProviders(Map<String, ExpressOnboardingCloudProvidersResponse> cloudProviders) {
        this.cloudProviders = cloudProviders;
    }

    @Override
    public String toString() {
        return "DetailedExpressOnboardingRegionsResponse{" +
                "cloudProviders=" + cloudProviders +
                '}';
    }
}
