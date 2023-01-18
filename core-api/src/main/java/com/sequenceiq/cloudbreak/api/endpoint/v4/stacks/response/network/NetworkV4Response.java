package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.NetworkV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class NetworkV4Response extends NetworkV4Base {

    @Schema(description = ModelDescriptions.ID)
    private Long id;

    @Schema(description = ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

}
