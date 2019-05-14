package com.sequenceiq.environment.environment.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.service.WorkspaceService;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentV1Request;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class EnvironmentV1RequestToEnvironmentConverter extends AbstractConversionServiceAwareConverter<EnvironmentV1Request, Environment> {

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public Environment convert(EnvironmentV1Request source) {
        Workspace defaultWorkspace = workspaceService.getDefaultWorkspace();
        Environment environment = new Environment();
        environment.setWorkspace(defaultWorkspace);
        environment.setName(source.getName());
        environment.setDescription(source.getDescription());
        return environment;
    }
}
