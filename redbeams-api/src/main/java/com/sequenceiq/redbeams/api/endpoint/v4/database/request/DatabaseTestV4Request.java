package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.ValidIfExactlyOneNonNull;
import com.sequenceiq.redbeams.api.endpoint.v4.database.base.DatabaseV4Identifiers;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseTest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_TEST_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidIfExactlyOneNonNull(fields = { "existingDatabase", "database" })
public class DatabaseTestV4Request implements Serializable {

    @Valid
    @Schema(description = DatabaseTest.EXISTING_REQUEST)
    private DatabaseV4Identifiers existingDatabase;

    @Valid
    @Schema(description = DatabaseTest.NEW_REQUEST)
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
