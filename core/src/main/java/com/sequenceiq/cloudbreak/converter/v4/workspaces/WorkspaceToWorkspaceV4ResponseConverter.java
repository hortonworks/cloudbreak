package com.sequenceiq.cloudbreak.converter.v4.workspaces;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceApiStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class WorkspaceToWorkspaceV4ResponseConverter extends AbstractConversionServiceAwareConverter<Workspace, WorkspaceV4Response> {

    @Override
    public WorkspaceV4Response convert(Workspace workspace) {
        WorkspaceV4Response json = new WorkspaceV4Response();
        json.setDescription(workspace.getDescription());
        json.setName(workspace.getName());
        json.setId(workspace.getId());
        json.setStatus(convertStatus(workspace.getStatus()));
        return json;
    }

    private WorkspaceApiStatus convertStatus(WorkspaceStatus status) {
        switch (status) {
            case ACTIVE:
                return WorkspaceApiStatus.ACTIVE;
            case DELETED:
            default:
                return WorkspaceApiStatus.DELETED;
        }
    }
}
