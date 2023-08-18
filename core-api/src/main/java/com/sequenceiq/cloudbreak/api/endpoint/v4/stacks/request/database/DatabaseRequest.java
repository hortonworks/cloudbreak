package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.DatabaseBase;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseRequest extends DatabaseBase {
    @ApiModelProperty(ModelDescriptions.Database.AZURE_DATABASE_REQUEST)
    @Deprecated
    private DatabaseAzureRequest databaseAzureRequest;

    @Deprecated
    public DatabaseAzureRequest getDatabaseAzureRequest() {
        return databaseAzureRequest;
    }

    @Deprecated
    public void setDatabaseAzureRequest(DatabaseAzureRequest databaseAzureRequest) {
        this.databaseAzureRequest = databaseAzureRequest;
    }
}
