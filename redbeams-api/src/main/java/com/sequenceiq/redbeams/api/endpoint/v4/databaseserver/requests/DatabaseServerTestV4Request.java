package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.ValidIfExactlyOneNonNull;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidIfExactlyOneNonNull(fields = { "existingDatabaseServerCrn", "databaseServer" })
public class DatabaseServerTestV4Request implements Serializable {

    @Valid
    @ApiModelProperty(DatabaseServer.DATABASE_SERVER_TEST_EXISTING_CRN)
    private String existingDatabaseServerCrn;

    @Valid
    @ApiModelProperty(DatabaseServer.DATABASE_SERVER_TEST_NEW_REQUEST)
    private DatabaseServerV4Request databaseServer;

    public String getExistingDatabaseServerCrn() {
        return existingDatabaseServerCrn;
    }

    public void setExistingDatabaseServerCrn(String existingDatabaseServerCrn) {
        this.existingDatabaseServerCrn = existingDatabaseServerCrn;
    }

    public DatabaseServerV4Request getDatabaseServer() {
        return databaseServer;
    }

    public void setDatabaseServer(DatabaseServerV4Request databaseServer) {
        this.databaseServer = databaseServer;
    }

}
