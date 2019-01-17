package com.sequenceiq.cloudbreak.converter.v4.workspaces;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserWorkspacePermissionsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;

@Component
public class WorkspaceToWorkspaceV4ResponseConverter extends AbstractConversionServiceAwareConverter<Workspace, WorkspaceV4Response> {

    @Inject
    private UserWorkspacePermissionsService userWorkspacePermissionService;

    @Override
    public WorkspaceV4Response convert(Workspace workspace) {
        WorkspaceV4Response json = new WorkspaceV4Response();
        json.setDescription(workspace.getDescription());
        json.setName(workspace.getName());
        json.setId(workspace.getId());
        json.setStatus(workspace.getStatus());
        Set<UserWorkspacePermissions> userPermissions = userWorkspacePermissionService.findForWorkspace(workspace);
        json.setUsers((Set<UserWorkspacePermissionsV4Response>) getConversionService().convert(userPermissions, TypeDescriptor.forObject(userPermissions),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(UserWorkspacePermissionsV4Response.class))));
        return json;
    }
}
