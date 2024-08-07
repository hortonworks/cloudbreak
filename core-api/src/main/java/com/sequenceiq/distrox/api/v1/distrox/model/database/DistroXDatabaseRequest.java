package com.sequenceiq.distrox.api.v1.distrox.model.database;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.Database;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DistroXDatabaseRequest extends DistroXDatabaseBase implements Serializable {
    @Schema(description = Database.AZURE_DATABASE_REQUEST)
    private DistroXDatabaseAzureRequest databaseAzureRequest;

    public DistroXDatabaseAzureRequest getDatabaseAzureRequest() {
        return databaseAzureRequest;
    }

    public void setDatabaseAzureRequest(DistroXDatabaseAzureRequest databaseAzureRequest) {
        this.databaseAzureRequest = databaseAzureRequest;
    }
}
