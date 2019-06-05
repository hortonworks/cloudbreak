package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseServerV4Identifiers implements Serializable {

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the database server's name must be between 5 and 100, inclusive")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The database server's name may only contain lowercase letters, digits, and hyphens, and must start with an alphanumeric character")
    @ApiModelProperty(value = DatabaseServer.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_ID, required = true)
    private String environmentId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

}
