package com.sequenceiq.environment.environment.v1;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Controller
@Transactional(TxType.NEVER)
public class EnvironmentController implements EnvironmentEndpoint {

    private final EnvironmentApiConverter environmentApiConverter;

    private final EnvironmentService environmentService;

    private final EnvironmentCreationService environmentCreationService;

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    private final EnvironmentModificationService environmentModificationService;

    public EnvironmentController(EnvironmentApiConverter environmentApiConverter, EnvironmentService environmentService,
            EnvironmentCreationService environmentCreationService, ThreadBasedUserCrnProvider threadBasedUserCrnProvider,
            EnvironmentModificationService environmentModificationService) {
        this.environmentApiConverter = environmentApiConverter;
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
        this.environmentModificationService = environmentModificationService;
    }

    @Override
    public DetailedEnvironmentResponse post(@Valid EnvironmentRequest request) {
        EnvironmentCreationDto environmentCreationDto = environmentApiConverter.initCreationDto(request);
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String creator = threadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto envDto = environmentCreationService.create(environmentCreationDto, accountId, creator);
        return environmentApiConverter.dtoToDetailedResponse(envDto);
    }

    @Override
    public DetailedEnvironmentResponse getByName(String environmentName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByNameAndAccountId(environmentName, accountId);
        return environmentApiConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    public DetailedEnvironmentResponse getByCrn(String crn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(crn, accountId);
        return environmentApiConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    public SimpleEnvironmentResponse deleteByName(String environmentName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environmentDto = environmentService.deleteByNameAndAccountId(environmentName, accountId, actualUserCrn);
        return environmentApiConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    public SimpleEnvironmentResponse deleteByCrn(String crn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environmentDto = environmentService.deleteByCrnAndAccountId(crn, accountId, actualUserCrn);
        return environmentApiConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    public SimpleEnvironmentResponses deleteMultipleByNames(Set<String> environmentNames) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentDto> environmentDtos = environmentService.deleteMultipleByNames(environmentNames, accountId, actualUserCrn);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentApiConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    public SimpleEnvironmentResponses deleteMultipleByCrns(Set<String> crns) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentDto> environmentDtos = environmentService.deleteMultipleByCrns(crns, accountId, actualUserCrn);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentApiConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    public DetailedEnvironmentResponse editByName(String environmentName, @NotNull EnvironmentEditRequest request) {
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        EnvironmentDto result = environmentModificationService.editByName(actualUserCrn, environmentName, editDto);
        return environmentApiConverter.dtoToDetailedResponse(result);
    }

    @Override
    public DetailedEnvironmentResponse editByCrn(String crn, @NotNull EnvironmentEditRequest request) {
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        EnvironmentDto result = environmentModificationService.editByCrn(actualUserCrn, crn, editDto);
        return environmentApiConverter.dtoToDetailedResponse(result);
    }

    @Override
    public SimpleEnvironmentResponses list() {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        List<EnvironmentDto> environmentDtos = environmentService.listByAccountId(accountId);
        List<SimpleEnvironmentResponse> responses = environmentDtos.stream().map(environmentApiConverter::dtoToSimpleResponse)
                .collect(Collectors.toList());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    public DetailedEnvironmentResponse changeCredentialByEnvironmentName(String environmentName, @Valid EnvironmentChangeCredentialRequest request) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentChangeCredentialDto dto = environmentApiConverter.convertEnvironmentChangeCredentialDto(request);
        EnvironmentDto result = environmentModificationService.changeCredentialByEnvironmentName(accountId, environmentName, dto);
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
