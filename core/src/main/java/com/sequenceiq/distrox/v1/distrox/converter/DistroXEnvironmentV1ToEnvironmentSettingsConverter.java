package com.sequenceiq.distrox.v1.distrox.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.model.environment.DistroXEnvironmentV1Request;

@Component
public class DistroXEnvironmentV1ToEnvironmentSettingsConverter {

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private WorkspaceService workspaceService;

    public EnvironmentSettingsV4Request convert(DistroXEnvironmentV1Request source) {
        EnvironmentView environmentView = environmentViewService.getByNameForWorkspace(source.getName(), workspaceService.getForCurrentUser());
        EnvironmentSettingsV4Request response = new EnvironmentSettingsV4Request();
        response.setCredentialName(environmentView.getCredential().getName());
        response.setName(source.getName());
        return response;
    }
}
