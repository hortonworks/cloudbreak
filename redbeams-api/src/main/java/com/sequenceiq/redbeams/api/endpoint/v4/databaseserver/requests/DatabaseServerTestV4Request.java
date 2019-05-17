package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.ValidIfExactlyOneNonNull;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

import javax.validation.Valid;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidIfExactlyOneNonNull(fields = { "existingDatabaseServerName", "databaseServer" })
public class DatabaseServerTestV4Request implements Serializable {

    @ApiModelProperty(DatabaseServer.NAME)
    private String existingDatabaseServerName;

    @Valid
    @ApiModelProperty(DatabaseServer.DATABASE_SERVER_REQUEST)
    private DatabaseServerV4Request databaseServer;

    public String getExistingDatabaseServerName() {
        return existingDatabaseServerName;
    }

    public void setExistingDatabaseServerName(String existingDatabaseServerName) {
        this.existingDatabaseServerName = existingDatabaseServerName;
    }

    public DatabaseServerV4Request getDatabaseServer() {
        return databaseServer;
    }

    public void setDatabaseServer(DatabaseServerV4Request databaseServer) {
        this.databaseServer = databaseServer;
    }

}
