package com.sequenceiq.cloudbreak.converter.environment;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class EnvironmentRequestToEnvironmentConverter extends AbstractConversionServiceAwareConverter<EnvironmentRequest, Environment> {

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public Environment convert(EnvironmentRequest source) {
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        Environment environment = new Environment();
        environment.setWorkspace(workspaceService.getByIdForCurrentUser(workspaceId));
        environment.setName(source.getName());
        environment.setDescription(source.getDescription());
        environment.setRegionsSet(source.getRegions());
        return environment;
    }
}
