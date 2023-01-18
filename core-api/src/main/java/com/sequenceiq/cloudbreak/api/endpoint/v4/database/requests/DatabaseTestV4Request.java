package com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.Database;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseTestV4Request implements JsonEntity {

    @Schema(description = Database.NAME)
    private String existingDatabaseName;

    @Valid
    @Schema(description = Database.DATABASE_REQUEST)
    private DatabaseV4Request database;

    public String getExistingDatabaseName() {
        return existingDatabaseName;
    }

    public void setExistingDatabaseName(String existingDatabaseName) {
        this.existingDatabaseName = existingDatabaseName;
    }

    public DatabaseV4Request getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseV4Request database) {
        this.database = database;
    }
}
