package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.AzureDatabaseType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Deprecated
public class DatabaseAzureRequest implements Serializable {
    @ApiModelProperty(ModelDescriptions.Database.AZURE_DATABASE_TYPE)
    @Deprecated
    private AzureDatabaseType azureDatabaseType;

    @Deprecated
    public AzureDatabaseType getAzureDatabaseType() {
        return azureDatabaseType == null ? AzureDatabaseType.SINGLE_SERVER : azureDatabaseType;
    }

    @Deprecated
    public void setAzureDatabaseType(AzureDatabaseType azureDatabaseType) {
        this.azureDatabaseType = azureDatabaseType;
    }
}
