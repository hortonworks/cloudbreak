package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DBStack;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@ApiModel(description = ModelDescriptions.TERMINATE_DATABASE_SERVER_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TerminateDatabaseServerV4Request {

    @Size(max = 40, min = 5, message = "The length of the name has to be in range of 5 to 40")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and must start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = DBStack.STACK_NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = DatabaseServer.ENVIRONMENT_CRN, required = true)
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
