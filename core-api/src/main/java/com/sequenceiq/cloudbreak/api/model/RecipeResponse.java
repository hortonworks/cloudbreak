package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RecipeResponse extends RecipeBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceResponse workspace;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkspaceResourceResponse getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceResponse workspace) {
        this.workspace = workspace;
    }
}
