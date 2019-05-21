package com.sequenceiq.environment.environment.v1.controller;

import static com.sequenceiq.environment.TempConstants.TEMP_ACCOUNT_ID;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.environment.api.WelcomeResponse;
import com.sequenceiq.environment.api.environment.v1.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.environment.v1.model.request.EnvironmentAttachRequest;
import com.sequenceiq.environment.api.environment.v1.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.environment.v1.model.request.EnvironmentDetachRequest;
import com.sequenceiq.environment.api.environment.v1.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.environment.v1.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.environment.v1.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.environment.v1.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.environment.v1.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.service.EnvironmentAttachService;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.environment.environment.v1.EnvironmentV1ApiConverter;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class EnvironmentV1Controller implements EnvironmentEndpoint {

    private final EnvironmentV1ApiConverter environmentV1ApiConverter;

    private final EnvironmentService environmentService;

    private final EnvironmentCreationService environmentCreationService;

    private final EnvironmentViewService environmentViewService;

    private final EnvironmentAttachService environmentAttachService;

    public EnvironmentV1Controller(EnvironmentV1ApiConverter environmentV1ApiConverter, EnvironmentService environmentService,
            EnvironmentCreationService environmentCreationService, EnvironmentViewService environmentViewService,
            EnvironmentAttachService environmentAttachService) {
        this.environmentV1ApiConverter = environmentV1ApiConverter;
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
        this.environmentViewService = environmentViewService;
        this.environmentAttachService = environmentAttachService;
    }

    @Override
    public WelcomeResponse welcome() {
        return new WelcomeResponse("");
    }

    @Override
    public DetailedEnvironmentResponse post(@Valid EnvironmentRequest request) {
        EnvironmentCreationDto environmentCreationDto = environmentV1ApiConverter.initCreationDto(request);
        EnvironmentDto environmentDto = environmentCreationService.create(environmentCreationDto);
        return environmentV1ApiConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    public DetailedEnvironmentResponse get(String environmentName) {
        return environmentService.get(environmentName);
    }

    @Override
    public SimpleEnvironmentResponse delete(String environmentName) {
        return environmentService.delete(environmentName);
    }

    @Override
    public DetailedEnvironmentResponse edit(String environmentName, @NotNull EnvironmentEditRequest request) {
        EnvironmentEditDto editDto = environmentV1ApiConverter.initEditDto(request);
        return environmentService.edit(environmentName, editDto);
    }

    @Override
    public SimpleEnvironmentResponses list() {
        return new SimpleEnvironmentResponses(environmentViewService.listByAccountId(TEMP_ACCOUNT_ID));
    }

    @Override
    public DetailedEnvironmentResponse attach(String environmentName, @Valid EnvironmentAttachRequest request) {
        return environmentAttachService.attachResources(environmentName, request);
    }

    @Override
    public DetailedEnvironmentResponse detach(String environmentName, @Valid EnvironmentDetachRequest request) {
        return environmentService.detachResources(environmentName, request);
    }

    @Override
    public DetailedEnvironmentResponse changeCredential(String environmentName, @Valid EnvironmentChangeCredentialRequest request) {
        // TODO implement
        return null;
    }
}
