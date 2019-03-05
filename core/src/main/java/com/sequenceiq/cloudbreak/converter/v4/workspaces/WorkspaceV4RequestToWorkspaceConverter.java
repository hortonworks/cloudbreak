package com.sequenceiq.cloudbreak.converter.v4.workspaces;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceStatus.ACTIVE;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.WorkspaceV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class WorkspaceV4RequestToWorkspaceConverter extends AbstractConversionServiceAwareConverter<WorkspaceV4Request, Workspace> {

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Workspace convert(WorkspaceV4Request source) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = new Workspace();
        workspace.setName(source.getName());
        workspace.setDescription(source.getDescription());
        workspace.setTenant(user.getTenant());
        workspace.setStatus(ACTIVE);
        return workspace;
    }
}
