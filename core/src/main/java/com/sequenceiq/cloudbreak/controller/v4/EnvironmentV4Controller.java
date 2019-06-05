package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.DatalakePrerequisiteV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.RegisterDatalakeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DatalakePrerequisiteV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakePrerequisiteService;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;

@Controller
@Transactional(TxType.NEVER)
public class EnvironmentV4Controller implements EnvironmentV4Endpoint {

    @Inject
    private DatalakePrerequisiteService datalakePrerequisiteService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Override
    public DetailedEnvironmentResponse post(Long workspaceId, @Valid EnvironmentRequest request) {
        return environmentClientService.create(request);
    }

    @Override
    public SimpleEnvironmentResponses list(Long workspaceId) {
        return environmentClientService.list();
    }

    @Override
    public DetailedEnvironmentResponse get(Long workspaceId, String environmentCrn) {
        return environmentClientService.get(environmentCrn);
    }

    @Override
    public SimpleEnvironmentResponse delete(Long workspaceId, String environmentCrn) {
        return environmentClientService.delete(environmentCrn);
    }

    @Override
    public DetailedEnvironmentResponse edit(Long workspaceId, String environmentCrn, @NotNull EnvironmentEditRequest request) {
        return environmentClientService.edit(environmentCrn, request);
    }

    @Override
    public DetailedEnvironmentV4Response attach(Long workspaceId, String environmentCrn, @Valid EnvironmentAttachV4Request request) {
        throw new UnsupportedOperationException("Attaching resource to an environment is not supported anymore!");
    }

    @Override
    public DetailedEnvironmentV4Response detach(Long workspaceId, String environmentCrn, @Valid EnvironmentDetachV4Request request) {
        throw new UnsupportedOperationException("Detaching resource from an environment is not supported anymore!");
    }

    @Override
    public DetailedEnvironmentV4Response registerExternalDatalake(Long workspaceId, String environmentCrn, @Valid RegisterDatalakeV4Request request) {
        throw new UnsupportedOperationException("registerExternalDatalake is not supported.");
    }

    @Override
    public DatalakePrerequisiteV4Response registerDatalakePrerequisite(Long workspaceId, String environmentName,
            @Valid DatalakePrerequisiteV4Request datalakePrerequisiteV4Request) {
        return datalakePrerequisiteService.prepare(workspaceId, environmentName, datalakePrerequisiteV4Request);
    }
}
