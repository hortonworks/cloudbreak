package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidIfExactlyOneNonNull;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServerTest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.DATABASE_SERVER_TEST_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidIfExactlyOneNonNull(fields = { "existingDatabaseServerCrn", "databaseServer" })
public class DatabaseServerTestV4Request implements Serializable {

    @ValidCrn(resource = CrnResourceDescriptor.DATABASE_SERVER)
    @ApiModelProperty(DatabaseServerTest.EXISTING_CRN)
    private String existingDatabaseServerCrn;

    @Valid
    @ApiModelProperty(DatabaseServerTest.NEW_REQUEST)
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
