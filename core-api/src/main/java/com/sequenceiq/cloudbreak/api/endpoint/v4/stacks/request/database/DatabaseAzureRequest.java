package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.AzureDatabaseType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseAzureRequest implements Serializable {
    @Schema(description = ModelDescriptions.Database.AZURE_DATABASE_TYPE)
    private AzureDatabaseType azureDatabaseType;

    @Schema(description = ModelDescriptions.Database.FLEXIBLE_SERVER_DELEGATED_SUBNET)
    private String flexibleServerDelegatedSubnetId;

    public AzureDatabaseType getAzureDatabaseType() {
        return azureDatabaseType == null ? AzureDatabaseType.FLEXIBLE_SERVER : azureDatabaseType;
    }

    public void setAzureDatabaseType(AzureDatabaseType azureDatabaseType) {
        this.azureDatabaseType = azureDatabaseType;
    }

    public void setFlexibleServerDelegatedSubnetId(String flexibleServerDelegatedSubnetId) {
        this.flexibleServerDelegatedSubnetId = flexibleServerDelegatedSubnetId;
    }

    public String getFlexibleServerDelegatedSubnetId() {
        return flexibleServerDelegatedSubnetId;
    }
}
