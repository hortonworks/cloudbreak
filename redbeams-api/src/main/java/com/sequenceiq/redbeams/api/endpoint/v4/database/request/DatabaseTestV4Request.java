package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.ValidIfExactlyOneNonNull;
import com.sequenceiq.redbeams.api.endpoint.v4.database.base.DatabaseV4Identifiers;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import java.io.Serializable;

import javax.validation.Valid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidIfExactlyOneNonNull(fields = { "existingDatabase", "database" })
public class DatabaseTestV4Request implements Serializable {

    @Valid
    @ApiModelProperty(Database.DATABASE_TEST_EXISTING_REQUEST)
    private DatabaseV4Identifiers existingDatabase;

    @Valid
    @ApiModelProperty(Database.DATABASE_TEST_NEW_REQUEST)
    private DatabaseV4Request database;

    public DatabaseV4Identifiers getExistingDatabase() {
        return existingDatabase;
    }

    public void setExistingDatabase(DatabaseV4Identifiers existingDatabase) {
        this.existingDatabase = existingDatabase;
    }

    public DatabaseV4Request getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseV4Request database) {
        this.database = database;
    }
}
