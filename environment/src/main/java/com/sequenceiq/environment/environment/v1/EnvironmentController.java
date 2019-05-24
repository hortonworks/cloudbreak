package com.sequenceiq.environment.environment.v1;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.environment.api.WelcomeResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAttachRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentDetachRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.service.EnvironmentAttachService;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class EnvironmentController implements EnvironmentEndpoint {

    private final EnvironmentApiConverter environmentApiConverter;

    private final EnvironmentService environmentService;

    private final EnvironmentCreationService environmentCreationService;

    private final EnvironmentViewService environmentViewService;

    private final EnvironmentAttachService environmentAttachService;

    private final AuthenticatedUserService authenticatedUserService;

    public EnvironmentController(EnvironmentApiConverter environmentApiConverter, EnvironmentService environmentService,
            EnvironmentCreationService environmentCreationService, EnvironmentViewService environmentViewService,
            EnvironmentAttachService environmentAttachService, AuthenticatedUserService authenticatedUserService) {
        this.environmentApiConverter = environmentApiConverter;
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
        this.environmentViewService = environmentViewService;
        this.environmentAttachService = environmentAttachService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    public WelcomeResponse welcome() {
        environmentCreationService.triggerCreationFlow();
        return new WelcomeResponse("welcome");
    }

    @Override
    public DetailedEnvironmentResponse post(@Valid EnvironmentRequest request) {
        EnvironmentCreationDto environmentCreationDto = environmentApiConverter.initCreationDto(request);
        EnvironmentDto environmentDto = environmentCreationService.create(environmentCreationDto);
        return environmentApiConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    public DetailedEnvironmentResponse get(String environmentName) {
        String accountId = authenticatedUserService.getAccountId();
        return environmentService.get(environmentName, accountId);
    }

    @Override
    public SimpleEnvironmentResponse delete(String environmentName) {
        String accountId = authenticatedUserService.getAccountId();
        return environmentService.delete(environmentName, accountId);
    }

    @Override
    public DetailedEnvironmentResponse edit(String environmentName, @NotNull EnvironmentEditRequest request) {
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        return environmentService.edit(environmentName, editDto);
    }

    @Override
    public SimpleEnvironmentResponses list() {
        String accountId = authenticatedUserService.getAccountId();
        return new SimpleEnvironmentResponses(environmentViewService.listByAccountId(accountId));
    }

    @Override
    public DetailedEnvironmentResponse attach(String environmentName, @Valid EnvironmentAttachRequest request) {
        return environmentAttachService.attachResources(environmentName, request);
    }

    @Override
    public DetailedEnvironmentResponse detach(String environmentName, @Valid EnvironmentDetachRequest request) {
        String accountId = authenticatedUserService.getAccountId();
        return environmentService.detachResources(environmentName, accountId, request);
    }

    @Override
    public DetailedEnvironmentResponse changeCredential(String environmentName, @Valid EnvironmentChangeCredentialRequest request) {
        // TODO implement
        return null;
    }
}
