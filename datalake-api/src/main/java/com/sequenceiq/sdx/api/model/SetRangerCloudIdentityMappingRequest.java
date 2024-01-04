package com.sequenceiq.sdx.api.model;

import java.util.Map;
import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SetRangerCloudIdentityMappingRequest {

    @NotNull
    @Schema(description = ModelDescriptions.AZURE_USER_MAPPING)
    private Map<String, String> azureUserMapping;

    /**
     * @deprecated azureGroupMapping is not supported
     */
    @Deprecated
    @Schema(description = ModelDescriptions.AZURE_GROUP_MAPPING)
    private Map<String, String> azureGroupMapping;

    public Map<String, String> getAzureUserMapping() {
        return azureUserMapping;
    }

    public void setAzureUserMapping(Map<String, String> azureUserMapping) {
        this.azureUserMapping = azureUserMapping;
    }

    public Map<String, String> getAzureGroupMapping() {
        return azureGroupMapping;
    }

    public void setAzureGroupMapping(Map<String, String> azureGroupMapping) {
        this.azureGroupMapping = azureGroupMapping;
    }

    @Override
    public String toString() {
        return "SetRangerCloudIdentityMappingRequest{" + "azureUserMapping=" + azureUserMapping + ", azureGroupMapping=" + azureGroupMapping + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SetRangerCloudIdentityMappingRequest that = (SetRangerCloudIdentityMappingRequest) o;
        return Objects.equals(azureUserMapping, that.azureUserMapping) && Objects.equals(azureGroupMapping, that.azureGroupMapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(azureUserMapping, azureGroupMapping);
    }
}
