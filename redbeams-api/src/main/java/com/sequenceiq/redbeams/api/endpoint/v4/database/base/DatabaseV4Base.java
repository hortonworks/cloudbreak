package com.sequenceiq.redbeams.api.endpoint.v4.database.base;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidJdbcConnectionUrl;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DatabaseV4Base implements Serializable {

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the database's name must be between 5 to 100, inclusive")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The database's name may only contain lowercase characters, digits, and hyphens, and must start with an alphanumeric character")
    @ApiModelProperty(value = Database.NAME, required = true)
    private String name;

    @Size(max = 1000000)
    @ApiModelProperty(Database.DESCRIPTION)
    private String description;

    @NotNull
    @ValidJdbcConnectionUrl
    @ApiModelProperty(value = Database.CONNECTION_URL, required = true)
    private String connectionURL;

    @NotNull
    @Size(min = 3, max = 56, message = "The length of the database's type must be between 3 and 56, inclusive")
    @Pattern(regexp = "(^[a-zA-Z_][-a-zA-Z0-9_]*[a-zA-Z0-9_]$)",
            message = "The database's type may only contain alphanumeric characters, underscores, and hyphens, and must "
                + "start with an alphanumeric character or underscore")
    @ApiModelProperty(value = Database.TYPE, required = true)
    private String type;

    @ApiModelProperty(value = Database.CONNECTION_DRIVER)
    private String connectionDriver;

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotNull
    @ApiModelProperty(Database.ENVIRONMENT_CRN)
    private String environmentCrn;

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }
}
