package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ExpressOnboardingRegionsResponse")
public class ExpressOnboardingCloudProvidersResponse implements Serializable {

    @Schema(description = "Regions by cloud providers")
    private List<ExpressOnboardingCloudProviderResponse> regions = new ArrayList<>();

    public ExpressOnboardingCloudProvidersResponse() {
        regions = new ArrayList<>();
    }

    public List<ExpressOnboardingCloudProviderResponse> getRegions() {
        return regions;
    }

    public void setRegions(List<ExpressOnboardingCloudProviderResponse> regions) {
        this.regions = regions;
    }

    @Override
    public String toString() {
        return "RegionResponse{" +
                "regions=" + regions +
                '}';
    }
}
