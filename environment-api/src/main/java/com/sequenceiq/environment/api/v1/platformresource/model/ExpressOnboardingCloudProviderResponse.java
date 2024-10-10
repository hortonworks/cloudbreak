package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ExpressOnboardingRegionResponse")
public class ExpressOnboardingCloudProviderResponse implements Serializable {

    @Schema(description = "Cloud provider side region")
    private String name;

    @Schema(description = "Display name of the region")
    private String label;

    @Schema(description = "CDP supported service")
    private List<String> services;

    public ExpressOnboardingCloudProviderResponse() {
        services = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    @Override
    public String toString() {
        return "RegionResponse{" +
                "name=" + name +
                ", label='" + label + '\'' +
                ", services=" + services +
                '}';
    }
}
