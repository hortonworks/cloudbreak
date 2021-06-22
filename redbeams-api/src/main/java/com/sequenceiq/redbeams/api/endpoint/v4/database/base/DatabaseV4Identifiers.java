package com.sequenceiq.redbeams.api.endpoint.v4.database.base;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.Database;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.DATABASE_IDENTIFIERS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseV4Identifiers implements Serializable {

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the database's name must be between 5 to 100, inclusive")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The database's name may only contain lowercase characters, digits, and hyphens, and must start with an alphanumeric character")
    @ApiModelProperty(value = Database.NAME, required = true)
    private String name;

    @NotNull
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @ApiModelProperty(value = Database.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

}
