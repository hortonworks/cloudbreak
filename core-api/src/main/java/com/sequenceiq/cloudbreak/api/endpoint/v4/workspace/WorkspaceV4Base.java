package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public abstract class WorkspaceV4Base implements JsonEntity {

    @Size(max = 100, min = 5, message = "The length of the workspace's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The workspace's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

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
}
