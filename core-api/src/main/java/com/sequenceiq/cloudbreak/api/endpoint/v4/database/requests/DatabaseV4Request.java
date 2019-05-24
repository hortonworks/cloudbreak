package com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.Database;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseV4Request extends DatabaseV4Base {

    @NotNull
    @ApiModelProperty(value = Database.USERNAME, required = true)
    private String connectionUserName;

    @NotNull
    @ApiModelProperty(value = Database.PASSWORD, required = true)
    private String connectionPassword;

    @ApiModelProperty(Database.ORACLE)
    private OracleParameters oracle;

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

    public OracleParameters getOracle() {
        return oracle;
    }

    public void setOracle(OracleParameters oracle) {
        this.oracle = oracle;
    }
}
