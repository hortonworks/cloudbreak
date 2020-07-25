package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetRangerCloudIdentityMappingRequest {

    @ApiModelProperty
    @NotNull
    private Map<String, String> azureUserMapping;

    /**
     * @deprecated azureGroupMapping is not unsupported
     */
    @Deprecated
    @ApiModelProperty
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SetRangerCloudIdentityMappingRequest that = (SetRangerCloudIdentityMappingRequest) o;
        return Objects.equals(azureUserMapping, that.azureUserMapping) &&
                Objects.equals(azureGroupMapping, that.azureGroupMapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(azureUserMapping, azureGroupMapping);
    }
}