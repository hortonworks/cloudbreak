package com.sequenceiq.distrox.v1.distrox.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.model.environment.DistroXEnvironmentV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class DistroXEnvironmentV1ToEnvironmentSettingsConverter {

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private WorkspaceService workspaceService;

    public EnvironmentSettingsV4Request convert(DistroXEnvironmentV1Request source) {
        DetailedEnvironmentResponse environment = environmentClientService.get(source.getName());
        EnvironmentSettingsV4Request response = new EnvironmentSettingsV4Request();
        response.setCredentialName(environment.getCredentialName());
        response.setName(source.getName());
        return response;
    }

    public DistroXEnvironmentV1Request convert(EnvironmentSettingsV4Request source) {
        DistroXEnvironmentV1Request response = new DistroXEnvironmentV1Request();
        response.setName(source.getName());
        return response;
    }
}
