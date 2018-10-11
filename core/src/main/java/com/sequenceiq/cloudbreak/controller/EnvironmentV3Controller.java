package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.EnvironmentV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class EnvironmentV3Controller implements EnvironmentV3Endpoint {

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public SimpleEnvironmentResponse create(Long workspaceId, @Valid EnvironmentRequest request) {
        return environmentService.createForLoggedInUser(request, workspaceId);
    }

    @Override
    public Set<SimpleEnvironmentResponse> list(Long workspaceId) {
        return environmentService.findAllByWorkspaceId(workspaceId).stream()
                .map(env -> conversionService.convert(env, SimpleEnvironmentResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public SimpleEnvironmentResponse get(Long workspaceId, String environmentName) {
        return conversionService.convert(environmentService.getByNameForWorkspaceId(environmentName, workspaceId), SimpleEnvironmentResponse.class);
    }

    // TODO: finish
    @Override
    public SimpleEnvironmentResponse attachResources(Long workspaceId, String environmentName, @Valid EnvironmentAttachRequest request) {
        return null;
    }

    // TODO: finish
    @Override
    public SimpleEnvironmentResponse detachResources(Long workspaceId, String environmentName, @Valid EnvironmentDetachRequest request) {
        return null;
    }
}
