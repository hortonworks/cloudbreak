package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.EnvironmentV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.RegisterDatalakeRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;

@Controller
@Transactional(TxType.NEVER)
public class EnvironmentV3Controller implements EnvironmentV3Endpoint {

    @Inject
    private EnvironmentService environmentService;

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
    public SimpleEnvironmentResponse delete(Long workspaceId, String environmentName) {
        return environmentService.delete(environmentName, workspaceId);
    }

    @Override
    public DetailedEnvironmentResponse attachResources(Long workspaceId, String environmentName, @Valid EnvironmentAttachRequest request) {
        return environmentService.attachResources(environmentName, request, workspaceId);
    }

    @Override
    public DetailedEnvironmentResponse detachResources(Long workspaceId, String environmentName, @Valid EnvironmentDetachRequest request) {
        return environmentService.detachResources(environmentName, request, workspaceId);
    }

    @Override
    public DetailedEnvironmentResponse changeCredential(Long workspaceId, String environmentName, EnvironmentChangeCredentialRequest request) {
        return environmentService.changeCredential(environmentName, workspaceId, request);
    }

    @Override
    public DetailedEnvironmentResponse registerExternalDatalake(Long workspaceId, String environmentName, @Valid RegisterDatalakeRequest request) {
        return environmentService.registerExternalDatalake(environmentName, workspaceId, request);
    }
}
