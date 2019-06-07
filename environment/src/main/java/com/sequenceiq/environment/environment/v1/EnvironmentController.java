package com.sequenceiq.environment.environment.v1;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;

@Controller
@Transactional(TxType.NEVER)
public class EnvironmentController implements EnvironmentEndpoint {

    private final EnvironmentApiConverter environmentApiConverter;

    private final EnvironmentService environmentService;

    private final EnvironmentCreationService environmentCreationService;

    private final EnvironmentViewService environmentViewService;

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    private final EnvironmentModificationService environmentModificationService;

    public EnvironmentController(EnvironmentApiConverter environmentApiConverter, EnvironmentService environmentService,
            EnvironmentCreationService environmentCreationService, EnvironmentViewService environmentViewService,
            ThreadBasedUserCrnProvider threadBasedUserCrnProvider, EnvironmentModificationService environmentModificationService) {
        this.environmentApiConverter = environmentApiConverter;
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
        this.environmentViewService = environmentViewService;
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
        this.environmentModificationService = environmentModificationService;
    }

    @Override
    public WelcomeResponse welcome() {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return new WelcomeResponse(accountId);
    }

    @Override
    public DetailedEnvironmentResponse post(@Valid EnvironmentRequest request) {
        EnvironmentCreationDto environmentCreationDto = environmentApiConverter.initCreationDto(request);
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentDto envDto = environmentCreationService.create(environmentCreationDto, accountId);
        environmentCreationService.triggerCreationFlow(envDto.getId(), envDto.getName());
        return environmentApiConverter.dtoToDetailedResponse(envDto);
    }

    @Override
    public DetailedEnvironmentResponse getByName(String environmentName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return environmentService.getByName(environmentName, accountId);
    }

    @Override
    public DetailedEnvironmentResponse getByCrn(String crn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return environmentService.getByCrn(crn, accountId);
    }

    @Override
    public SimpleEnvironmentResponse deleteByName(String environmentName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return environmentService.deleteByName(environmentName, accountId);
    }

    @Override
    public SimpleEnvironmentResponse deleteByCrn(String crn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return environmentService.deleteByCrn(crn, accountId);
    }

    @Override
    public SimpleEnvironmentResponses deleteMultipleByNames(Set<String> environmentNames) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return environmentService.deleteMultipleByNames(environmentNames, accountId);
    }

    @Override
    public SimpleEnvironmentResponses deleteMultipleByCrns(Set<String> crns) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return environmentService.deleteMultipleByCrns(crns, accountId);
    }

    @Override
    public DetailedEnvironmentResponse editByName(String environmentName, @NotNull EnvironmentEditRequest request) {
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        EnvironmentDto result = environmentModificationService.editByName(environmentName, editDto);
        return environmentApiConverter.dtoToDetailedResponse(result);
    }

    @Override
    public DetailedEnvironmentResponse editByCrn(String crn, @NotNull EnvironmentEditRequest request) {
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        EnvironmentDto result = environmentModificationService.editByCrn(crn, editDto);
        return environmentApiConverter.dtoToDetailedResponse(result);
    }

    @Override
    public SimpleEnvironmentResponses list() {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return new SimpleEnvironmentResponses(environmentViewService.listByAccountId(accountId));
    }

    @Override
    public DetailedEnvironmentResponse changeCredentialByEnvironmentName(String environmentName, @Valid EnvironmentChangeCredentialRequest request) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentDto result = environmentModificationService.changeCredentialByEnvironmentName(accountId, environmentName,
                environmentApiConverter.convertEnvironmentChangeCredentialDto(request));
        return environmentApiConverter.dtoToDetailedResponse(result);
    }

    @Override
    public DetailedEnvironmentResponse changeCredentialByEnvironmentCrn(String crn, @Valid EnvironmentChangeCredentialRequest request) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentDto result = environmentModificationService.changeCredentialByEnvironmentCrn(accountId, crn,
                environmentApiConverter.convertEnvironmentChangeCredentialDto(request));
        return environmentApiConverter.dtoToDetailedResponse(result);
    }
}
