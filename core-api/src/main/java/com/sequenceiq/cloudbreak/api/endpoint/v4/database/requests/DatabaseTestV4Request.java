package com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.Database;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseTestV4Request implements JsonEntity {

    @ApiModelProperty(Database.NAME)
    private String existingDatabaseName;

    @Valid
    @ApiModelProperty(Database.DATABASE_REQUEST)
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
