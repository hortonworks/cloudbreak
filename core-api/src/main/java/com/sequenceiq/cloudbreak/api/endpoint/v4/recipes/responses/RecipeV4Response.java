package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.RecipeModelDescription.DESCRIPTION)
public class RecipeV4Response extends RecipeV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }
}
