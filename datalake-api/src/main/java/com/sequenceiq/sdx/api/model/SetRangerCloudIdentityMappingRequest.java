package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.Map;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetRangerCloudIdentityMappingRequest {

    @ApiModelProperty
    @NotNull
    private Map<String, String> azureUserMapping;

    @ApiModelProperty
    @NotNull
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
        return "SetRangerCloudIdentityMappingRequest{" +
                "azureUserMapping=" + azureUserMapping +
                ", azureGroupMapping=" + azureGroupMapping +
                '}';
    }
}