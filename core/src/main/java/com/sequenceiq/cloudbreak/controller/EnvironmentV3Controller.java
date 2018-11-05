package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.EnvironmentV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
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
    public DetailedEnvironmentResponse create(Long workspaceId, @Valid EnvironmentRequest request) {
        return environmentService.createForLoggedInUser(request, workspaceId);
    }

    @Override
    public Set<SimpleEnvironmentResponse> list(Long workspaceId) {
        return environmentService.listByWorkspaceId(workspaceId);
    }

    @Override
    public DetailedEnvironmentResponse get(Long workspaceId, String environmentName) {
        return environmentService.get(environmentName, workspaceId);
    }

    @Override
    public DetailedEnvironmentResponse delete(Long workspaceId, String environmentName) {
        return environmentService.delete(environmentName, workspaceId);
    }

    @Override
    public DetailedEnvironmentResponse attachResources(Long workspaceId, String environmentName, @Valid EnvironmentAttachRequest request) {
        return environmentService.attachResources(environmentName, request, workspaceId);
    }

    // TODO: finish
    @Override
    public DetailedEnvironmentResponse detachResources(Long workspaceId, String environmentName, @Valid EnvironmentDetachRequest request) {
        return environmentService.detachResources(environmentName, request, workspaceId);
    }

    @Override
    public DetailedEnvironmentResponse changeCredential(Long workspaceId, String environmentName, EnvironmentChangeCredentialRequest request) {
        return environmentService.changeCredential(environmentName, workspaceId, request);
    }
}
