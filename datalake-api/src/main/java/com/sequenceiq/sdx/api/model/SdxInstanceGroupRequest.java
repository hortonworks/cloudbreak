package com.sequenceiq.sdx.api.model;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxInstanceGroupRequest {

    @NotNull
    @Schema(description = ModelDescriptions.INSTANCE_GROUP_NAME)
    private String name;

    @Schema(description = ModelDescriptions.INSTANCE_TYPE)
    private String instanceType;

    @Schema(description = ModelDescriptions.FALLBACK_INSTANCE_TYPES)
    private List<String> fallbackInstanceTypes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public List<String> getFallbackInstanceTypes() {
        return fallbackInstanceTypes;
    }

    public void setFallbackInstanceTypes(List<String> fallbackInstanceTypes) {
        this.fallbackInstanceTypes = fallbackInstanceTypes;
    }
}
