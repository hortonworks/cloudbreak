package com.sequenceiq.distrox.v1.distrox.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.distrox.api.v1.distrox.model.environment.DistroXEnvironmentV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DistroXEnvironmentV1ToEnvironmentSettingsConverter {

    @Inject
    private EnvironmentClientService environmentClientService;

    public EnvironmentSettingsV4Request convert(DistroXEnvironmentV1Request source) {
        DetailedEnvironmentResponse environment = environmentClientService.getByName(source.getName());
        EnvironmentSettingsV4Request response = new EnvironmentSettingsV4Request();
        response.setCredentialName(environment.getCredential().getName());
        response.setName(source.getName());
        return response;
    }

    public DistroXEnvironmentV1Request convert(EnvironmentSettingsV4Request source) {
        DistroXEnvironmentV1Request response = new DistroXEnvironmentV1Request();
        response.setName(source.getName());
        return response;
    }
}
