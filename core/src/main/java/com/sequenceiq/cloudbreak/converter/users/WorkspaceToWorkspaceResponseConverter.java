package com.sequenceiq.cloudbreak.converter.users;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResponse;
import com.sequenceiq.cloudbreak.api.model.users.UserWorkspacePermissionsJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;

@Component
public class WorkspaceToWorkspaceResponseConverter extends AbstractConversionServiceAwareConverter<Workspace, WorkspaceResponse> {

    @Inject
    private UserWorkspacePermissionsService userWorkspacePermissionService;

    @Override
    public WorkspaceResponse convert(Workspace workspace) {
        WorkspaceResponse json = new WorkspaceResponse();
        json.setDescription(workspace.getDescription());
        json.setName(workspace.getName());
        json.setId(workspace.getId());
        json.setStatus(workspace.getStatus());
        Set<UserWorkspacePermissions> userPermissions = userWorkspacePermissionService.findForWorkspace(workspace);
        json.setUsers((Set<UserWorkspacePermissionsJson>) getConversionService().convert(userPermissions, TypeDescriptor.forObject(userPermissions),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(UserWorkspacePermissionsJson.class))));
        return json;
    }
}
