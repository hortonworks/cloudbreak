package com.sequenceiq.cloudbreak.converter.users;

import static com.sequenceiq.cloudbreak.api.model.v2.WorkspaceStatus.ACTIVE;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.WorkspaceRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class WorkspaceRequestToWorkspaceConverter extends AbstractConversionServiceAwareConverter<WorkspaceRequest, Workspace> {

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Workspace convert(WorkspaceRequest source) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = new Workspace();
        workspace.setName(source.getName());
        workspace.setDescription(source.getDescription());
        workspace.setTenant(user.getTenant());
        workspace.setStatus(ACTIVE);
        return workspace;
    }
}
