package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses;

import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecipeModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class WorkspaceResourceV4Response implements JsonEntity {

    @ApiModelProperty(RecipeModelDescription.WORKSPACE_ID)
    private Long id;

    @ApiModelProperty(RecipeModelDescription.WORKSPACE_NAME)
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
