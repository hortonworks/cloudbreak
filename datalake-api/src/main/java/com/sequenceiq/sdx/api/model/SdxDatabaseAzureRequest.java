package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.AzureDatabaseType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseAzureRequest {
    @Schema(description = ModelDescriptions.AZURE_DATABASE_TYPE)
    private AzureDatabaseType azureDatabaseType = AzureDatabaseType.SINGLE_SERVER;

    @Schema(description = ModelDescriptions.AZURE_DATABASE_TYPE)
    private String flexibleServerDelegatedSubnetId;

    public AzureDatabaseType getAzureDatabaseType() {
        return azureDatabaseType == null ? AzureDatabaseType.SINGLE_SERVER : azureDatabaseType;
    }

    public void setAzureDatabaseType(AzureDatabaseType azureDatabaseType) {
        this.azureDatabaseType = azureDatabaseType;
    }

    public String getFlexibleServerDelegatedSubnetId() {
        return flexibleServerDelegatedSubnetId;
    }

    public void setFlexibleServerDelegatedSubnetId(String flexibleServerDelegatedSubnetId) {
        this.flexibleServerDelegatedSubnetId = flexibleServerDelegatedSubnetId;
    }
}
