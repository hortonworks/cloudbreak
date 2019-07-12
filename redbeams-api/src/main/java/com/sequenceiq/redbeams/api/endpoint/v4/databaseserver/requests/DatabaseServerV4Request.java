package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base.DatabaseServerV4Base;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;
import com.sequenceiq.redbeams.validation.ValidConnectorJarUrlForDatabaseVendor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidConnectorJarUrlForDatabaseVendor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseServerV4Request extends DatabaseServerV4Base {

    @NotNull
    @ApiModelProperty(value = DatabaseServer.CONNECTION_USER_NAME, required = true)
    private String connectionUserName;

    @NotNull
    @ApiModelProperty(value = DatabaseServer.CONNECTION_PASSWORD, required = true)
    private String connectionPassword;

    // @ApiModelProperty(Database.ORACLE)
    // private OracleParameters oracle;

    public String getConnectionUserName() {
        return connectionUserName;
    }

    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName = connectionUserName;
    }

    public String getConnectionPassword() {
        return connectionPassword;
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    // public OracleParameters getOracle() {
    //     return oracle;
    // }

    // public void setOracle(OracleParameters oracle) {
    //     this.oracle = oracle;
    // }
}
