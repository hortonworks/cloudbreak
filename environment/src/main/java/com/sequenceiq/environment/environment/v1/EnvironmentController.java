package com.sequenceiq.environment.environment.v1;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentCrnResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentDeletionService;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStartService;
import com.sequenceiq.environment.environment.service.EnvironmentStopService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentApiConverter;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;

@Controller
@InternalReady
@Transactional(TxType.NEVER)
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
public class EnvironmentController implements EnvironmentEndpoint {

    private final EnvironmentApiConverter environmentApiConverter;

    private final EnvironmentResponseConverter environmentResponseConverter;

    private final EnvironmentService environmentService;

    private final EnvironmentCreationService environmentCreationService;

    private final EnvironmentDeletionService environmentDeletionService;

    private final EnvironmentModificationService environmentModificationService;

    private final EnvironmentStartService environmentStartService;

    private final EnvironmentStopService environmentStopService;

    private final CredentialService credentialService;

    public EnvironmentController(
            EnvironmentApiConverter environmentApiConverter,
            EnvironmentResponseConverter environmentResponseConverter,
            EnvironmentService environmentService,
            EnvironmentCreationService environmentCreationService,
            EnvironmentDeletionService environmentDeletionService,
            EnvironmentModificationService environmentModificationService,
            EnvironmentStartService environmentStartService,
            EnvironmentStopService environmentStopService,
            CredentialService credentialService) {
        this.environmentApiConverter = environmentApiConverter;
        this.environmentResponseConverter = environmentResponseConverter;
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
        this.environmentDeletionService = environmentDeletionService;
        this.environmentModificationService = environmentModificationService;
        this.environmentStartService = environmentStartService;
        this.environmentStopService = environmentStopService;
        this.credentialService = credentialService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    @CheckPermissionByResourceObject
    public DetailedEnvironmentResponse post(@ResourceObject @Valid EnvironmentRequest request) {
        EnvironmentCreationDto environmentCreationDto = environmentApiConverter.initCreationDto(request);
        EnvironmentDto envDto = environmentCreationService.create(environmentCreationDto);
        return environmentResponseConverter.dtoToDetailedResponse(envDto);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DetailedEnvironmentResponse getByName(@ResourceName String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByNameAndAccountId(environmentName, accountId);
        return environmentResponseConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public EnvironmentCrnResponse getCrnByName(@ResourceName String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String crn = environmentService.getCrnByNameAndAccountId(environmentName, accountId);
        return environmentApiConverter.crnResponse(environmentName, crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DetailedEnvironmentResponse getByCrn(@ResourceCrn @TenantAwareParam String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(crn, accountId);
        return environmentResponseConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public SimpleEnvironmentResponse deleteByName(@ResourceName String environmentName, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environmentDto = environmentDeletionService.deleteByNameAndAccountId(environmentName, accountId, actualUserCrn, forced);
        return environmentResponseConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public SimpleEnvironmentResponse deleteByCrn(@ResourceCrn@TenantAwareParam String crn, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        EnvironmentDto environmentDto = environmentDeletionService.deleteByCrnAndAccountId(crn, accountId, actualUserCrn, forced);
        return environmentResponseConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public SimpleEnvironmentResponses deleteMultipleByNames(@ResourceNameList Set<String> environmentNames, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentDto> environmentDtos = environmentDeletionService.deleteMultipleByNames(environmentNames, accountId, actualUserCrn, forced);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentResponseConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public SimpleEnvironmentResponses deleteMultipleByCrns(@ResourceCrnList Set<String> crns, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentDto> environmentDtos = environmentDeletionService.deleteMultipleByCrns(crns, accountId, actualUserCrn, forced);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentResponseConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public DetailedEnvironmentResponse editByName(@ResourceName String environmentName, @NotNull EnvironmentEditRequest request) {
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        EnvironmentDto result = environmentModificationService.editByName(environmentName, editDto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public DetailedEnvironmentResponse editByCrn(@ResourceCrn String crn, @NotNull EnvironmentEditRequest request) {
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(request);
        EnvironmentDto result = environmentModificationService.editByCrn(crn, editDto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @FilterListBasedOnPermissions(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public SimpleEnvironmentResponses list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        List<EnvironmentDto> environmentDtos = environmentService.listByAccountId(accountId);
        List<SimpleEnvironmentResponse> responses = environmentDtos.stream().map(environmentResponseConverter::dtoToSimpleResponse)
                .collect(Collectors.toList());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    @CheckPermissionByResourceObject
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.CHANGE_CREDENTIAL)
    public DetailedEnvironmentResponse changeCredentialByEnvironmentName(@ResourceName String environmentName,
            @ResourceObject @Valid EnvironmentChangeCredentialRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentChangeCredentialDto dto = environmentApiConverter.convertEnvironmentChangeCredentialDto(request);
        EnvironmentDto result = environmentModificationService.changeCredentialByEnvironmentName(accountId, environmentName, dto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentName(@ResourceName String name, @Valid FeaturesRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentFeatures features = environmentApiConverter.convertToEnvironmentTelemetryFeatures(request);
        EnvironmentDto result = environmentModificationService.changeTelemetryFeaturesByEnvironmentName(accountId, name, features);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceObject
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.CHANGE_CREDENTIAL)
    public DetailedEnvironmentResponse changeCredentialByEnvironmentCrn(@ResourceCrn String crn,
            @ResourceObject @Valid EnvironmentChangeCredentialRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto result = environmentModificationService.changeCredentialByEnvironmentCrn(accountId, crn,
                environmentApiConverter.convertEnvironmentChangeCredentialDto(request));
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentCrn(@ResourceCrn String crn, @Valid FeaturesRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentFeatures features = environmentApiConverter.convertToEnvironmentTelemetryFeatures(request);
        EnvironmentDto result = environmentModificationService.changeTelemetryFeaturesByEnvironmentCrn(accountId, crn, features);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.START_ENVIRONMENT)
    public void postStartByName(@ResourceName String name) {
        environmentStartService.startByName(name);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_ENVIRONMENT)
    public void postStartByCrn(@ResourceCrn @TenantAwareParam String crn) {
        environmentStartService.startByCrn(crn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.STOP_ENVIRONMENT)
    public void postStopByName(@ResourceName String name) {
        environmentStopService.stopByName(name);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_ENVIRONMENT)
    public void postStopByCrn(@ResourceCrn @TenantAwareParam String crn) {
        environmentStopService.stopByCrn(crn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public Object getCreateEnvironmentForCliByName(@ResourceName String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByNameAndAccountId(environmentName, accountId);
        return environmentService.getCreateEnvironmentForCli(environmentDto);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public Object getCreateEnvironmentForCliByCrn(@ResourceCrn @TenantAwareParam String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(crn, accountId);
        return environmentService.getCreateEnvironmentForCli(environmentDto);
    }

    @Override
    @DisableCheckPermissions
    public Object getCreateEnvironmentForCli(EnvironmentRequest environmentRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(environmentRequest.getCredentialName(), accountId);
        return environmentService.getCreateEnvironmentForCli(environmentRequest, credential.getCloudPlatform());
    }
}
