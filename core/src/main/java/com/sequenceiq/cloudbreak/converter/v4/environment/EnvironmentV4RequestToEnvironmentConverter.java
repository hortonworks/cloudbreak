package com.sequenceiq.cloudbreak.converter.v4.environment;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class EnvironmentV4RequestToEnvironmentConverter extends AbstractConversionServiceAwareConverter<EnvironmentV4Request, Environment> {

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public Environment convert(EnvironmentV4Request source) {
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        Environment environment = new Environment();
        environment.setWorkspace(workspaceService.getByIdForCurrentUser(workspaceId));
        environment.setName(source.getName());
        environment.setDescription(source.getDescription());
        return environment;
    }
}
