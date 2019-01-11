package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.DatalakePrerequisiteV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentChangeCredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.RegisterDatalakeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DatalakePrerequisiteV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Responses;
import com.sequenceiq.cloudbreak.service.datalake.DatalakePrerequisiteService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;

@Controller
@Transactional(TxType.NEVER)
public class EnvironmentV4Controller implements EnvironmentV4Endpoint {

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private DatalakePrerequisiteService datalakePrerequisiteService;

    @Override
    public DetailedEnvironmentV4Response post(Long workspaceId, @Valid EnvironmentV4Request request) {
        return environmentService.createForLoggedInUser(request, workspaceId);
    }

    @Override
    public SimpleEnvironmentV4Responses list(Long workspaceId) {
        return new SimpleEnvironmentV4Responses(environmentService.listByWorkspaceId(workspaceId));
    }

    @Override
    public DetailedEnvironmentV4Response get(Long workspaceId, String environmentName) {
        return environmentService.get(environmentName, workspaceId);
    }

    @Override
    public SimpleEnvironmentV4Response delete(Long workspaceId, String environmentName) {
        return environmentService.delete(environmentName, workspaceId);
    }

    @Override
    public DetailedEnvironmentV4Response attach(Long workspaceId, String environmentName, @Valid EnvironmentAttachV4Request request) {
        return environmentService.attachResources(environmentName, request, workspaceId);
    }

    @Override
    public DetailedEnvironmentV4Response detach(Long workspaceId, String environmentName, @Valid EnvironmentDetachV4Request request) {
        return environmentService.detachResources(environmentName, request, workspaceId);
    }

    @Override
    public DetailedEnvironmentV4Response changeCredential(Long workspaceId, String environmentName, @Valid EnvironmentChangeCredentialV4Request request) {
        return environmentService.changeCredential(environmentName, workspaceId, request);
    }

    @Override
    public DetailedEnvironmentV4Response registerExternalDatalake(Long workspaceId, String environmentName, @Valid RegisterDatalakeV4Request request) {
        return environmentService.registerExternalDatalake(environmentName, workspaceId, request);
    }

    @Override
    public DatalakePrerequisiteV4Response registerDatalakePrerequisite(Long workspaceId, String environmentName,
            @Valid DatalakePrerequisiteV4Request datalakePrerequisiteV4Request) {
        return datalakePrerequisiteService.prepare(workspaceId, environmentName, datalakePrerequisiteV4Request);
    }
}
