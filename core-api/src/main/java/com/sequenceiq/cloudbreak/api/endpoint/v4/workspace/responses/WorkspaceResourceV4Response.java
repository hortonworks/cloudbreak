package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecipeModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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
