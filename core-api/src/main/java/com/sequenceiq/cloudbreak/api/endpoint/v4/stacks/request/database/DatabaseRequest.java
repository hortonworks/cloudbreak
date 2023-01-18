package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.DatabaseBase;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseRequest extends DatabaseBase {
    @Schema(description = ModelDescriptions.Database.AZURE_DATABASE_REQUEST)
    private DatabaseAzureRequest databaseAzureRequest;

    public DatabaseAzureRequest getDatabaseAzureRequest() {
        return databaseAzureRequest;
    }

    public void setDatabaseAzureRequest(DatabaseAzureRequest databaseAzureRequest) {
        this.databaseAzureRequest = databaseAzureRequest;
    }
}
