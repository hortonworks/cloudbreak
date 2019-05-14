package com.sequenceiq.environment.environment.controller;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.environment.api.environment.endpoint.EnvironmentV1Endpoint;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentAttachV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentChangeCredentialV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentDetachV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentEditV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentV1Request;
import com.sequenceiq.environment.api.environment.model.response.DetailedEnvironmentV1Response;
import com.sequenceiq.environment.api.environment.model.response.SimpleEnvironmentV1Response;
import com.sequenceiq.environment.api.environment.model.response.SimpleEnvironmentV1Responses;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class EnvironmentV1Controller implements EnvironmentV1Endpoint {

    @Inject
    private EnvironmentService environmentService;

    @Override
    public DetailedEnvironmentV1Response post(@Valid EnvironmentV1Request request) {
        return environmentService.createForLoggedInUser(request);
    }

    @Override
    public DetailedEnvironmentV1Response get(String environmentName) {
        return environmentService.get(environmentName);
    }

    @Override
    public SimpleEnvironmentV1Response delete(String environmentName) {
        return environmentService.delete(environmentName);
    }

    @Override
    public DetailedEnvironmentV1Response edit(String environmentName, @NotNull EnvironmentEditV1Request request) {
        return environmentService.edit(environmentName, request);
    }

    @Override
    public SimpleEnvironmentV1Responses list() {
        return new SimpleEnvironmentV1Responses(environmentService.listByWorkspaceId());
    }

    @Override
    public DetailedEnvironmentV1Response attach(String environmentName, @Valid EnvironmentAttachV1Request request) {
        return environmentService.attachResources(environmentName, request);
    }

    @Override
    public DetailedEnvironmentV1Response detach(String environmentName, @Valid EnvironmentDetachV1Request request) {
        return environmentService.detachResources(environmentName, request);
    }

    @Override
    public DetailedEnvironmentV1Response changeCredential(String environmentName, @Valid EnvironmentChangeCredentialV1Request request) {
        return environmentService.changeCredential(environmentName, request);
    }
}
