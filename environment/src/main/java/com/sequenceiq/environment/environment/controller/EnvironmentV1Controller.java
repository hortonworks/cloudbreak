package com.sequenceiq.environment.environment.controller;

import static com.sequenceiq.environment.TempConstants.TEMP_ACCOUNT_ID;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.environment.api.WelcomeResponse;
import com.sequenceiq.environment.api.environment.endpoint.EnvironmentV1Endpoint;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentAttachV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentChangeCredentialV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentDetachV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentEditV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentV1Request;
import com.sequenceiq.environment.api.environment.model.response.DetailedEnvironmentV1Response;
import com.sequenceiq.environment.api.environment.model.response.SimpleEnvironmentV1Response;
import com.sequenceiq.environment.api.environment.model.response.SimpleEnvironmentV1Responses;
import com.sequenceiq.environment.env.service.EnvironmentCreationService;
import com.sequenceiq.environment.env.service.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class EnvironmentV1Controller implements EnvironmentV1Endpoint {

    private final EnvironmentService environmentService;

    private final EnvironmentCreationService environmentCreationService;

    public EnvironmentV1Controller(EnvironmentService environmentService, EnvironmentCreationService environmentCreationService) {
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
    }

    @Override
    public WelcomeResponse welcome() {
        EnvironmentDto environment = environmentCreationService.createEnvironment(EnvironmentDto.EnvironmentDtoBuilder.anEnvironmentDto()
                .build());
        return new WelcomeResponse(environment.getName());
    }

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
        return new SimpleEnvironmentV1Responses(environmentService.listByAccountId(TEMP_ACCOUNT_ID));
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
        // TODO implement
        return null;
    }
}
