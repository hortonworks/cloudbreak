package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;
import com.sequenceiq.redbeams.validation.ValidDatabaseVendorAndService;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.DATABASE_REQUEST)
@ValidDatabaseVendorAndService
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseV4Request extends DatabaseV4Base {

    @NotNull
    @ApiModelProperty(value = Database.USERNAME, required = true)
    private String connectionUserName;

    @NotNull
    @ApiModelProperty(value = Database.PASSWORD, required = true)
    private String connectionPassword;

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

}
