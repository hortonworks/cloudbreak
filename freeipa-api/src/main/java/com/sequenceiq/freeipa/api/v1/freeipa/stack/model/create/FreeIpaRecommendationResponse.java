package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaRecommendationV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaRecommendationResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<VmTypeResponse> vmTypes = new HashSet<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<VmTypeResponse> deprecatedVmTypes = new HashSet<>();

    private String defaultInstanceType;

    public FreeIpaRecommendationResponse() {
    }

    public FreeIpaRecommendationResponse(Set<VmTypeResponse> vmTypes, Set<VmTypeResponse> deprecatedVmTypes, String defaultInstanceType) {
        this.vmTypes = vmTypes;
        this.deprecatedVmTypes = deprecatedVmTypes;
        this.defaultInstanceType = defaultInstanceType;
    }

    public Set<VmTypeResponse> getVmTypes() {
        return vmTypes;
    }

    public void setVmTypes(Set<VmTypeResponse> vmTypes) {
        this.vmTypes = vmTypes;
    }

    public Set<VmTypeResponse> getDeprecatedVmTypes() {
        return deprecatedVmTypes;
    }

    public void setDeprecatedVmTypes(Set<VmTypeResponse> deprecatedVmTypes) {
        this.deprecatedVmTypes = deprecatedVmTypes;
    }

    public String getDefaultInstanceType() {
        return defaultInstanceType;
    }

    public void setDefaultInstanceType(String defaultInstanceType) {
        this.defaultInstanceType = defaultInstanceType;
    }

    @Override
    public String toString() {
        return "FreeIpaRecommendationResponse{" +
                "vmTypes=" + vmTypes +
                ", deprecatedVmTypes=" + deprecatedVmTypes +
                ", defaultInstanceType='" + defaultInstanceType + '\'' +
                '}';
    }
}
