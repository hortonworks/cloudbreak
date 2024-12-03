package com.sequenceiq.redbeams.api.endpoint.v4.database.base;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidJdbcConnectionUrl;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DatabaseV4Base implements Serializable {

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the database's name must be between 5 to 100, inclusive")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The database's name may only contain lowercase characters, digits, and hyphens, and must start with an alphanumeric character")
    @Schema(description = Database.NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 1000000)
    @Schema(description = Database.DESCRIPTION)
    private String description;

    @NotNull
    @ValidJdbcConnectionUrl
    @Schema(description = Database.CONNECTION_URL, requiredMode = Schema.RequiredMode.REQUIRED)
    private String connectionURL;

    @NotNull
    @Size(min = 3, max = 56, message = "The length of the database's type must be between 3 and 56, inclusive")
    @Pattern(regexp = "(^[a-zA-Z_][-a-zA-Z0-9_]*[a-zA-Z0-9_]$)",
            message = "The database's type may only contain alphanumeric characters, underscores, and hyphens, and must "
                + "start with an alphanumeric character or underscore")
    @Schema(description = Database.TYPE, requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Schema(description = Database.CONNECTION_DRIVER)
    private String connectionDriver;

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotNull
    @Schema(description = Database.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
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
