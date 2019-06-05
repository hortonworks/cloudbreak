package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.ValidIfExactlyOneNonNull;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base.DatabaseServerV4Identifiers;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

import javax.validation.Valid;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidIfExactlyOneNonNull(fields = { "existingDatabaseServer", "databaseServer" })
public class DatabaseServerTestV4Request implements Serializable {

    @Valid
    @ApiModelProperty(DatabaseServer.DATABASE_SERVER_TEST_EXISTING_REQUEST)
    private DatabaseServerV4Identifiers existingDatabaseServer;

    @Valid
    @ApiModelProperty(DatabaseServer.DATABASE_SERVER_TEST_NEW_REQUEST)
    private DatabaseServerV4Request databaseServer;

    public DatabaseServerV4Identifiers getExistingDatabaseServer() {
        return existingDatabaseServer;
    }

    public void setExistingDatabaseServer(DatabaseServerV4Identifiers existingDatabaseServer) {
        this.existingDatabaseServer = existingDatabaseServer;
    }

    public DatabaseServerV4Request getDatabaseServer() {
        return databaseServer;
    }

    public void setDatabaseServer(DatabaseServerV4Request databaseServer) {
        this.databaseServer = databaseServer;
    }

}
