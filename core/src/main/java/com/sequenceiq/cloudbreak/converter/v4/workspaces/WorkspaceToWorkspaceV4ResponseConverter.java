package com.sequenceiq.cloudbreak.converter.v4.workspaces;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@Component
public class WorkspaceToWorkspaceV4ResponseConverter extends AbstractConversionServiceAwareConverter<Workspace, WorkspaceV4Response> {

    @Override
    public WorkspaceV4Response convert(Workspace workspace) {
        WorkspaceV4Response json = new WorkspaceV4Response();
        json.setDescription(workspace.getDescription());
        json.setName(workspace.getName());
        json.setId(workspace.getId());
        json.setStatus(workspace.getStatus());
        return json;
    }
}
