package com.sequenceiq.distrox.api.v1.distrox.model.database;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.Database;
import com.sequenceiq.common.model.AzureDatabaseType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DistroXDatabaseAzureRequest implements Serializable {
    @Schema(description = Database.AZURE_DATABASE_TYPE)
    private AzureDatabaseType azureDatabaseType;

    @Schema(description = Database.FLEXIBLE_SERVER_DELEGATED_SUBNET)
    private String flexibleServerDelegatedSubnetId;

    public AzureDatabaseType getAzureDatabaseType() {
        return azureDatabaseType == null ? AzureDatabaseType.FLEXIBLE_SERVER : azureDatabaseType;
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
