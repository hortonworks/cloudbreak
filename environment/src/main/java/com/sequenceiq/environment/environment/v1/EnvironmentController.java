package com.sequenceiq.environment.environment.v1;

import java.util.Set;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.environment.api.WelcomeResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
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

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    public EnvironmentController(EnvironmentApiConverter environmentApiConverter, EnvironmentService environmentService,
            EnvironmentCreationService environmentCreationService, EnvironmentViewService environmentViewService,
            ThreadBasedUserCrnProvider threadBasedUserCrnProvider, AuthenticatedUserService authenticatedUserService) {
        this.environmentApiConverter = environmentApiConverter;
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
        this.environmentViewService = environmentViewService;
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
    }

    @Override
    public WelcomeResponse welcome() {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return new WelcomeResponse(accountId);
    }

    @Override
    public DetailedEnvironmentResponse post(@Valid EnvironmentRequest request) {
        EnvironmentCreationDto environmentCreationDto = environmentApiConverter.initCreationDto(request);
        EnvironmentDto envDto = environmentCreationService.create(environmentCreationDto);
        environmentCreationService.triggerCreationFlow(envDto.getId(), envDto.getName());
        return environmentApiConverter.dtoToDetailedResponse(envDto);
    }

    @Override
    public DetailedEnvironmentResponse get(String environmentName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return environmentService.get(environmentName, accountId);
    }

    @Override
    public SimpleEnvironmentResponse delete(String environmentName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return environmentService.delete(environmentName, accountId);
    }

    @Override
    public SimpleEnvironmentResponses deleteMultiple(Set<String> environmentNames) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return environmentService.deleteMultiple(environmentNames, accountId);
    }

    @Override
    public DetailedEnvironmentResponse edit(String environmentName, @NotNull EnvironmentEditRequest request) {
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        return environmentService.edit(environmentName, editDto);
    }

    @Override
    public SimpleEnvironmentResponses list() {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return new SimpleEnvironmentResponses(environmentViewService.listByAccountId(accountId));
    }

    @Override
    public DetailedEnvironmentResponse changeCredential(String environmentName, @Valid EnvironmentChangeCredentialRequest request) {
        throw new NotImplementedException("changing credential is not supported yet");
    }

}
