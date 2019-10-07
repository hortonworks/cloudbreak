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
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.credential.model.response.EmptyResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Controller
@InternalReady
@Transactional(TxType.NEVER)
public class EnvironmentController implements EnvironmentEndpoint {

    private final EnvironmentApiConverter environmentApiConverter;

    private final EnvironmentService environmentService;

    private final EnvironmentCreationService environmentCreationService;

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    private final EnvironmentModificationService environmentModificationService;

    private final CredentialService credentialService;

    public EnvironmentController(
            EnvironmentApiConverter environmentApiConverter,
            EnvironmentService environmentService,
            EnvironmentCreationService environmentCreationService,
            ThreadBasedUserCrnProvider threadBasedUserCrnProvider,
            EnvironmentModificationService environmentModificationService,
            CredentialService credentialService) {
        this.environmentApiConverter = environmentApiConverter;
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
        this.environmentModificationService = environmentModificationService;
        this.credentialService = credentialService;
    }

    @Override
    public DetailedEnvironmentResponse post(@Valid EnvironmentRequest request) {
        EnvironmentCreationDto environmentCreationDto = environmentApiConverter.initCreationDto(request);
        EnvironmentDto envDto = environmentCreationService.create(environmentCreationDto);
        return environmentApiConverter.dtoToDetailedResponse(envDto);
    }

    @Override
    public DetailedEnvironmentResponse getByName(String environmentName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByNameAndAccountId(environmentName, accountId);
        return environmentApiConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    public DetailedEnvironmentResponse getByCrn(@ResourceCrn String crn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(crn, accountId);
        return environmentApiConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    public SimpleEnvironmentResponse deleteByName(String environmentName, boolean forced) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environmentDto = environmentService.deleteByNameAndAccountId(environmentName, accountId, actualUserCrn, forced);
        return environmentApiConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    public SimpleEnvironmentResponse deleteByCrn(String crn, boolean forced) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environmentDto = environmentService.deleteByCrnAndAccountId(crn, accountId, actualUserCrn, forced);
        return environmentApiConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    public SimpleEnvironmentResponses deleteMultipleByNames(Set<String> environmentNames, boolean forced) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentDto> environmentDtos = environmentService.deleteMultipleByNames(environmentNames, accountId, actualUserCrn, forced);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentApiConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    public SimpleEnvironmentResponses deleteMultipleByCrns(Set<String> crns, boolean forced) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = threadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentDto> environmentDtos = environmentService.deleteMultipleByCrns(crns, accountId, actualUserCrn, forced);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentApiConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
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

    @Override
    public Object getCreateEnvironmentForCliByName(String environmentName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByNameAndAccountId(environmentName, accountId);
        if (!CloudPlatform.AWS.name().equals(environmentDto.getCloudPlatform())) {
            return new EmptyResponse();
        }
        return environmentService.getCreateAWSEnvironmentForCli(environmentDto);
    }

    @Override
    public Object getCreateEnvironmentForCliByCrn(String crn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(crn, accountId);
        if (!CloudPlatform.AWS.name().equals(environmentDto.getCloudPlatform())) {
            return new EmptyResponse();
        }
        return environmentService.getCreateAWSEnvironmentForCli(environmentDto);
    }

    @Override
    public Object getCreateEnvironmentForCli(EnvironmentRequest environmentRequest) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(environmentRequest.getCredentialName(), accountId);
        if (!CloudPlatform.AWS.name().equals(credential.getCloudPlatform())) {
            return new EmptyResponse();
        }
        return environmentService.getCreateAWSEnvironmentForCli(environmentRequest, credential.getCloudPlatform());
    }
}
