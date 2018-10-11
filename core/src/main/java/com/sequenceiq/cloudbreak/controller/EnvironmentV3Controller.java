package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

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

    @Override
    public SimpleEnvironmentResponse create(Long workspaceId, @Valid EnvironmentRequest request) {
        return environmentService.createForLoggedInUser(request, workspaceId);
    }

    // TODO: finish
    @Override
    public SimpleEnvironmentResponse get(Long workspaceId, String environmentName) {

        return null;
    }

    // TODO: finish
    @Override
    public Set<SimpleEnvironmentResponse> list(Long workspaceId) {
        return null;
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
