package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import static com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseTestV4Request implements Serializable {

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
