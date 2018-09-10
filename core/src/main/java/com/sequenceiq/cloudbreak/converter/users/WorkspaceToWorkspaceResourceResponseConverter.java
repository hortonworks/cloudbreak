package com.sequenceiq.cloudbreak.converter.users;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@Component
public class WorkspaceToWorkspaceResourceResponseConverter extends AbstractConversionServiceAwareConverter<Workspace, WorkspaceResourceResponse> {

    @Override
    public WorkspaceResourceResponse convert(Workspace source) {
        WorkspaceResourceResponse workspace = new WorkspaceResourceResponse();
        workspace.setName(source.getName());
        workspace.setId(source.getId());
        return workspace;
    }
}
