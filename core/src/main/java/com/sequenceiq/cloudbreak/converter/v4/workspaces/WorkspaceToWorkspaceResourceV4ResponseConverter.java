package com.sequenceiq.cloudbreak.converter.v4.workspaces;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class WorkspaceToWorkspaceResourceV4ResponseConverter {

    public WorkspaceResourceV4Response convert(Workspace source) {
        WorkspaceResourceV4Response workspace = new WorkspaceResourceV4Response();
        workspace.setName(source.getName());
        workspace.setId(source.getId());
        return workspace;
    }
}
