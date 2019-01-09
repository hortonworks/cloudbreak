package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel
public class WorkspaceV4Responses extends WorkspaceV4Base {

    private Set<WorkspaceV4Response> workspaces;

    public Set<WorkspaceV4Response> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(Set<WorkspaceV4Response> workspaces) {
        this.workspaces = workspaces;
    }

    public static final WorkspaceV4Responses workspaceV4Responses(Set<WorkspaceV4Response> workspaces) {
        WorkspaceV4Responses workspaceV4Responses = new WorkspaceV4Responses();
        workspaceV4Responses.setWorkspaces(workspaces);
        return workspaceV4Responses;
    }

}
