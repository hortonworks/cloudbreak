package com.sequenceiq.environment.environment.v1;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_RECIPE;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN_LIST;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME_LIST;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.type.DataHubStartAction;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentCloudStorageValidationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentLoadBalancerUpdateRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.UpdateAzureResourceEncryptionParametersRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.CreateEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentCrnResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentDatabaseServerCertificateStatusV4Response;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.environment.api.v1.environment.model.response.OutboundTypeValidationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.api.v1.environment.model.response.SupportedOperatingSystemResponse;
import com.sequenceiq.environment.authorization.EnvironmentFiltering;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.CreateEnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.dto.UpdateAzureResourceEncryptionDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentDeletionService;
import com.sequenceiq.environment.environment.service.EnvironmentLoadBalancerService;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStackConfigUpdateService;
import com.sequenceiq.environment.environment.service.EnvironmentStartService;
import com.sequenceiq.environment.environment.service.EnvironmentStopService;
import com.sequenceiq.environment.environment.service.EnvironmentUpgradeCcmService;
import com.sequenceiq.environment.environment.service.EnvironmentUpgradeOutboundService;
import com.sequenceiq.environment.environment.service.EnvironmentVerticalScaleService;
import com.sequenceiq.environment.environment.service.SupportedOperatingSystemService;
import com.sequenceiq.environment.environment.service.cloudstorage.CloudStorageValidator;
import com.sequenceiq.environment.environment.service.database.RedBeamsService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeFlowService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentApiConverter;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.service.FlowProgressService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Responses;

@Controller
@Transactional(TxType.NEVER)
@AccountEntityType(Environment.class)
public class EnvironmentController implements EnvironmentEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentController.class);

    private final EnvironmentApiConverter environmentApiConverter;

    private final EnvironmentResponseConverter environmentResponseConverter;

    private final EnvironmentService environmentService;

    private final EnvironmentCreationService environmentCreationService;

    private final EnvironmentDeletionService environmentDeletionService;

    private final EnvironmentModificationService environmentModificationService;

    private final EnvironmentStartService environmentStartService;

    private final EnvironmentStopService environmentStopService;

    private final FreeIpaService freeIpaService;

    private final RedBeamsService redBeamsService;

    private final CredentialService credentialService;

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    private final EnvironmentStackConfigUpdateService stackConfigUpdateService;

    private final EnvironmentLoadBalancerService environmentLoadBalancerService;

    private final FlowProgressService flowProgressService;

    private final EnvironmentFiltering environmentFiltering;

    private final CloudStorageValidator cloudStorageValidator;

    private final EnvironmentUpgradeCcmService upgradeCcmService;

    private final EnvironmentVerticalScaleService environmentVerticalScaleService;

    private final StackV4Endpoint stackV4Endpoint;

    private final SupportedOperatingSystemService supportedOperatingSystemService;

    private final ExternalizedComputeFlowService externalizedComputeFlowService;

    private final EnvironmentUpgradeOutboundService environmentUpgradeOutboundService;

    public EnvironmentController(
            EnvironmentApiConverter environmentApiConverter,
            EnvironmentResponseConverter environmentResponseConverter,
            EnvironmentService environmentService,
            EnvironmentCreationService environmentCreationService,
            EnvironmentDeletionService environmentDeletionService,
            FlowProgressService flowProgressService,
            EnvironmentModificationService environmentModificationService,
            EnvironmentStartService environmentStartService,
            EnvironmentStopService environmentStopService,
            FreeIpaService freeIpaService,
            CredentialService credentialService,
            CredentialToCredentialV1ResponseConverter credentialConverter,
            EnvironmentStackConfigUpdateService stackConfigUpdateService,
            EnvironmentLoadBalancerService environmentLoadBalancerService,
            EnvironmentFiltering environmentFiltering,
            CloudStorageValidator cloudStorageValidator,
            EnvironmentUpgradeCcmService upgradeCcmService,
            EnvironmentVerticalScaleService environmentVerticalScaleService,
            StackV4Endpoint stackV4Endpoint,
            SupportedOperatingSystemService supportedOperatingSystemService,
            ExternalizedComputeFlowService externalizedComputeFlowService,
            EnvironmentReactorFlowManager environmentReactorFlowManager,
            RedBeamsService redBeamsService,
            EnvironmentUpgradeOutboundService environmentUpgradeOutboundService) {
        this.environmentApiConverter = environmentApiConverter;
        this.environmentResponseConverter = environmentResponseConverter;
        this.environmentService = environmentService;
        this.environmentCreationService = environmentCreationService;
        this.environmentDeletionService = environmentDeletionService;
        this.flowProgressService = flowProgressService;
        this.environmentModificationService = environmentModificationService;
        this.environmentStartService = environmentStartService;
        this.environmentStopService = environmentStopService;
        this.freeIpaService = freeIpaService;
        this.credentialService = credentialService;
        this.credentialConverter = credentialConverter;
        this.stackConfigUpdateService = stackConfigUpdateService;
        this.environmentLoadBalancerService = environmentLoadBalancerService;
        this.environmentFiltering = environmentFiltering;
        this.cloudStorageValidator = cloudStorageValidator;
        this.upgradeCcmService = upgradeCcmService;
        this.environmentVerticalScaleService = environmentVerticalScaleService;
        this.stackV4Endpoint = stackV4Endpoint;
        this.supportedOperatingSystemService = supportedOperatingSystemService;
        this.externalizedComputeFlowService = externalizedComputeFlowService;
        this.redBeamsService = redBeamsService;
        this.environmentUpgradeOutboundService = environmentUpgradeOutboundService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "credentialName", type = NAME, action = DESCRIBE_CREDENTIAL)
    @CheckPermissionByRequestProperty(path = "encryptionProfileCrn", type = CRN, action = DESCRIBE_ENCRYPTION_PROFILE, skipOnNull = true)
    @CheckPermissionByRequestProperty(path = "freeIpa.recipes", type = NAME_LIST, action = DESCRIBE_RECIPE, skipOnNull = true)
    public CreateEnvironmentResponse post(@RequestObject EnvironmentRequest request) {
        EnvironmentCreationDto environmentCreationDto = environmentApiConverter.initCreationDto(request);
        CreateEnvironmentDto envDto = environmentCreationService.create(environmentCreationDto);
        return environmentResponseConverter.dtoToCreateResponse(envDto);
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_ENVIRONMENT)
    public DetailedEnvironmentResponse getByName(@ResourceName String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByNameAndAccountId(environmentName, accountId);
        return environmentResponseConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_ENVIRONMENT)
    public Map<String, Set<String>> getAttachedExperiencesByEnvironmentName(@ResourceName String name) {
        return environmentService.collectExperiences(NameOrCrn.ofName(name));
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_ENVIRONMENT)
    public EnvironmentCrnResponse getCrnByName(@ResourceName String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String crn = environmentService.getCrnByNameAndAccountId(environmentName, accountId);
        return environmentApiConverter.crnResponse(environmentName, crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public DetailedEnvironmentResponse getByCrn(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(crn, accountId);
        return environmentResponseConverter.dtoToDetailedResponse(environmentDto);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public Map<String, Set<String>> getAttachedExperiencesByEnvironmentCrn(@ResourceCrn String crn) {
        return environmentService.collectExperiences(NameOrCrn.ofCrn(crn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public SimpleEnvironmentResponse deleteByName(@ResourceName String environmentName, boolean cascading, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        EnvironmentViewDto environmentDto = environmentDeletionService.deleteByNameAndAccountId(
                environmentName, accountId, actualUserCrn, cascading, forced);
        return environmentResponseConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public SimpleEnvironmentResponse deleteByCrn(@ResourceCrn String crn, boolean cascading, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        EnvironmentViewDto environmentDto = environmentDeletionService.deleteByCrnAndAccountId(
                crn, accountId, actualUserCrn, cascading, forced);
        return environmentResponseConverter.dtoToSimpleResponse(environmentDto);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public SimpleEnvironmentResponses deleteMultipleByNames(@ResourceNameList Set<String> environmentNames, boolean cascading, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentViewDto> environmentDtos = environmentDeletionService.deleteMultipleByNames(
                environmentNames, accountId, actualUserCrn, cascading, forced);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentResponseConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public SimpleEnvironmentResponses deleteMultipleByCrns(@ResourceCrnList Set<String> crns, boolean cascading, boolean forced) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String actualUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<EnvironmentViewDto> environmentDtos = environmentDeletionService.deleteMultipleByCrns(
                crns, accountId, actualUserCrn, cascading, forced);
        Set<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(environmentResponseConverter::dtoToSimpleResponse).collect(Collectors.toSet());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public DetailedEnvironmentResponse editByName(@ResourceName String environmentName, EnvironmentEditRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Environment environment = environmentModificationService.getEnvironment(accountId, NameOrCrn.ofName(environmentName));
        if (environment.getStatus() != EnvironmentStatus.AVAILABLE) {
            throw new BadRequestException("Environment status is not AVAILABLE for Edit");
        }
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(environment, request);
        EnvironmentDto result = environmentModificationService.edit(environment, editDto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public DetailedEnvironmentResponse editByCrn(@ResourceCrn String crn, EnvironmentEditRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Environment environment = environmentModificationService.getEnvironment(accountId, NameOrCrn.ofCrn(crn));
        EnvironmentEditDto editDto = environmentApiConverter.initEditDto(environment, request);
        EnvironmentDto result = environmentModificationService.edit(environment, editDto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @FilterListBasedOnPermissions
    public SimpleEnvironmentResponses list(String remoteEnvironmentCrn) {
        List<EnvironmentDto> environmentDtos = environmentFiltering.filterEnvironments(
                DESCRIBE_ENVIRONMENT,
                Optional.ofNullable(remoteEnvironmentCrn)
        );
        return toSimpleEnvironmentResponses(environmentDtos);
    }

    @Override
    @InternalOnly
    public SimpleEnvironmentResponses listInternal(@AccountId String accountId) {
        return listAllEnvironmentsForAccount(accountId);
    }

    private SimpleEnvironmentResponses listAllEnvironmentsForAccount(String accountId) {
        List<EnvironmentDto> environmentDtos = environmentService.listByAccountId(accountId);
        return toSimpleEnvironmentResponses(environmentDtos);
    }

    private SimpleEnvironmentResponses toSimpleEnvironmentResponses(List<EnvironmentDto> environmentDtos) {
        List<SimpleEnvironmentResponse> responses = environmentDtos.stream()
                .map(c -> environmentResponseConverter.dtoToSimpleResponse(c, true, true))
                .collect(Collectors.toList());
        return new SimpleEnvironmentResponses(responses);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "credentialName", type = NAME, action = DESCRIBE_CREDENTIAL)
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.CHANGE_CREDENTIAL)
    public DetailedEnvironmentResponse changeCredentialByEnvironmentName(@ResourceName String environmentName,
            @RequestObject EnvironmentChangeCredentialRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Environment environment = environmentModificationService.getEnvironment(accountId, NameOrCrn.ofName(environmentName));
        if (environment.getStatus() != EnvironmentStatus.AVAILABLE) {
            throw new BadRequestException("Environment status is not AVAILABLE for Edit");
        }
        EnvironmentChangeCredentialDto dto = environmentApiConverter.convertEnvironmentChangeCredentialDto(request);
        EnvironmentDto result = environmentModificationService.changeCredentialByEnvironmentName(accountId, environmentName, dto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentName(@ResourceName String name, FeaturesRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentFeatures features = environmentApiConverter.convertToEnvironmentTelemetryFeatures(request);
        EnvironmentDto result = environmentModificationService.changeTelemetryFeaturesByEnvironmentName(accountId, name, features);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "credentialName", type = NAME, action = DESCRIBE_CREDENTIAL)
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.CHANGE_CREDENTIAL)
    public DetailedEnvironmentResponse changeCredentialByEnvironmentCrn(@ResourceCrn String crn, @RequestObject EnvironmentChangeCredentialRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto result = environmentModificationService.changeCredentialByEnvironmentCrn(accountId, crn,
                environmentApiConverter.convertEnvironmentChangeCredentialDto(request));
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPDATE_AZURE_ENCRYPTION_RESOURCES)
    public DetailedEnvironmentResponse updateAzureResourceEncryptionParametersByEnvironmentCrn(@ResourceCrn String crn,
            @RequestObject UpdateAzureResourceEncryptionParametersRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto result = environmentModificationService.updateAzureResourceEncryptionParametersByEnvironmentCrn(accountId, crn,
                environmentApiConverter.convertUpdateAzureResourceEncryptionDto(request));
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPDATE_AZURE_ENCRYPTION_RESOURCES)
    public DetailedEnvironmentResponse updateAzureResourceEncryptionParametersByEnvironmentName(@ResourceName String environmentName,
            @RequestObject UpdateAzureResourceEncryptionParametersRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        UpdateAzureResourceEncryptionDto dto = environmentApiConverter.convertUpdateAzureResourceEncryptionDto(request);
        EnvironmentDto result = environmentModificationService.updateAzureResourceEncryptionParametersByEnvironmentName(accountId, environmentName, dto);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentCrn(@ResourceCrn String crn, FeaturesRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentFeatures features = environmentApiConverter.convertToEnvironmentTelemetryFeatures(request);
        EnvironmentDto result = environmentModificationService.changeTelemetryFeaturesByEnvironmentCrn(accountId, crn, features);
        return environmentResponseConverter.dtoToDetailedResponse(result);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.START_ENVIRONMENT)
    public FlowIdentifier postStartByName(@ResourceName String name, DataHubStartAction dataHubStartAction) {
        return environmentStartService.startByName(name, dataHubStartAction);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_ENVIRONMENT)
    public FlowIdentifier postStartByCrn(@ResourceCrn String crn, DataHubStartAction dataHubStartAction) {
        return environmentStartService.startByCrn(crn, dataHubStartAction);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.STOP_ENVIRONMENT)
    public FlowIdentifier postStopByName(@ResourceName String name) {
        return environmentStopService.stopByName(name);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_ENVIRONMENT)
    public FlowIdentifier postStopByCrn(@ResourceCrn String crn) {
        return environmentStopService.stopByCrn(crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public CredentialResponse verifyCredentialByEnvCrn(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentCrnAndAccountId(crn, accountId, ENVIRONMENT);
        Credential verifiedCredential = credentialService.verify(credential);
        return credentialConverter.convert(verifiedCredential);
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_ENVIRONMENT)
    public Object getCreateEnvironmentForCliByName(@ResourceName String environmentName) {
        throw new UnsupportedOperationException("not supported request");
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public Object getCreateEnvironmentForCliByCrn(@ResourceCrn String crn) {
        throw new UnsupportedOperationException("not supported request");
    }

    @Override
    @CheckPermissionByRequestProperty(type = NAME, action = DESCRIBE_ENVIRONMENT, path = "name")
    public Object getCreateEnvironmentForCli(@RequestObject EnvironmentRequest environmentRequest) {
        throw new UnsupportedOperationException("not supported request");
    }

    @Override
    @InternalOnly
    public FlowIdentifier updateConfigsInEnvironmentByCrn(@ResourceCrn String crn) {
        return stackConfigUpdateService.updateAllStackConfigsByCrn(crn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public FlowIdentifier updateEnvironmentLoadBalancersByName(@ResourceName String envName, EnvironmentLoadBalancerUpdateRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByNameAndAccountId(envName, accountId);
        EnvironmentLoadBalancerDto environmentLoadBalancerDto = environmentApiConverter.initLoadBalancerDto(request);
        return environmentLoadBalancerService.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLoadBalancerDto);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public FlowIdentifier updateEnvironmentLoadBalancersByCrn(@ResourceCrn String crn, EnvironmentLoadBalancerUpdateRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(crn, accountId);
        EnvironmentLoadBalancerDto environmentLoadBalancerDto = environmentApiConverter.initLoadBalancerDto(request);
        return environmentLoadBalancerService.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLoadBalancerDto);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public FlowProgressResponse getLastFlowLogProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return flowProgressService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return flowProgressService.getFlowProgressListByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "credentialCrn", type = CRN, action = DESCRIBE_CREDENTIAL)
    public ObjectStorageValidateResponse validateCloudStorage(
            @RequestObject EnvironmentCloudStorageValidationRequest environmentCloudStorageValidationRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return cloudStorageValidator.validateCloudStorage(accountId, environmentCloudStorageValidationRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.UPGRADE_CCM)
    public FlowIdentifier upgradeCcmByName(@ResourceName String name) {
        return upgradeCcmService.upgradeCcmByName(name);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPGRADE_CCM)
    public FlowIdentifier upgradeCcmByCrn(@ResourceCrn String crn) {
        return upgradeCcmService.upgradeCcmByCrn(crn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.ENVIRONMENT_VERTICAL_SCALING)
    public FlowIdentifier verticalScalingByName(@ResourceName String name, @RequestObject VerticalScaleRequest updateRequest) {
        return environmentVerticalScaleService.verticalScaleByName(name, updateRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ENVIRONMENT_VERTICAL_SCALING)
    public FlowIdentifier verticalScalingByCrn(@ResourceCrn String crn, @RequestObject VerticalScaleRequest updateRequest) {
        return environmentVerticalScaleService.verticalScaleByCrn(crn, updateRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public boolean isUpgradeCcmAvailable(@ResourceCrn String crn) {
        EnvironmentDto environmentDto = environmentService.internalGetByCrn(crn);
        return Tunnel.getUpgradables().contains(environmentDto.getTunnel()) ||
                environmentDto.getTunnel() == Tunnel.latestUpgradeTarget() &&
                        ThreadBasedUserCrnProvider.doAsInternalActor(
                                () -> stackV4Endpoint.getNotCcmUpgradedStackCount(0L, crn, ThreadBasedUserCrnProvider.getUserCrn()) > 0);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public OutboundTypeValidationResponse validateOutboundTypes(@ResourceCrn String crn) {
        return environmentUpgradeOutboundService.validateOutboundTypes(crn);
    }

    @Override
    // We can disable permission since it does not contain sensitive information
    @DisableCheckPermissions
    public SupportedOperatingSystemResponse listSupportedOperatingSystem(String cloudPlatform) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return supportedOperatingSystemService.listSupportedOperatingSystem(accountId, cloudPlatform);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrns", type = CRN_LIST, action = DESCRIBE_ENVIRONMENT, skipOnNull = true)
    public EnvironmentDatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(
            @RequestObject EnvironmentDatabaseServerCertificateStatusV4Request request) {
        EnvironmentDatabaseServerCertificateStatusV4Responses responses = new EnvironmentDatabaseServerCertificateStatusV4Responses();
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        DatabaseServerCertificateStatusV4Responses databaseServerCertificateStatusV4Responses =
                redBeamsService.listDatabaseServersCertificateStatusByEnvironmentCrns(request, userCrn);
        for (DatabaseServerCertificateStatusV4Response response : databaseServerCertificateStatusV4Responses.getResponses()) {
            EnvironmentDatabaseServerCertificateStatusV4Response databaseServerCertificateStatusV4Response
                    = new EnvironmentDatabaseServerCertificateStatusV4Response();
            databaseServerCertificateStatusV4Response.setSslStatus(response.getSslStatus());
            databaseServerCertificateStatusV4Response.setEnvironmentCrn(response.getEnvironmentCrn());
            responses.getResponses().add(databaseServerCertificateStatusV4Response);
        }
        return responses;
    }
}
