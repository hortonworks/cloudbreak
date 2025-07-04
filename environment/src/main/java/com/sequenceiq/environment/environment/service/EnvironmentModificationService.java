package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentSetupCrossRealmTrustRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.EnvironmentHybridDto;
import com.sequenceiq.environment.environment.dto.EnvironmentTagsDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.UpdateAzureResourceEncryptionDto;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.encryption.EnvironmentEncryptionService;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.environment.validation.ValidationType;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.AzureParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.repository.AzureParametersRepository;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigModificationService;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneNetwork;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class EnvironmentModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentModificationService.class);

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentService environmentService;

    private final CredentialService credentialService;

    private final NetworkService networkService;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    private final ParametersService parametersService;

    private final EnvironmentFlowValidatorService environmentFlowValidatorService;

    private final EnvironmentResourceService environmentResourceService;

    private final EnvironmentEncryptionService environmentEncryptionService;

    private final AzureParametersRepository azureParametersRepository;

    private final DnsV1Endpoint dnsV1Endpoint;

    private final ProxyConfigService proxyConfigService;

    private final ProxyConfigModificationService proxyConfigModificationService;

    private final EnvironmentReactorFlowManager environmentReactorFlowManager;

    private final EnvironmentTagsDtoConverter environmentTagsDtoConverter;

    private final EnvironmentValidatorService environmentValidatorService;

    private final EventSenderService eventSenderService;

    private final FreeIpaService freeIpaService;

    public EnvironmentModificationService(
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentService environmentService,
            CredentialService credentialService,
            NetworkService networkService,
            AuthenticationDtoConverter authenticationDtoConverter,
            ParametersService parametersService,
            EnvironmentFlowValidatorService environmentFlowValidatorService,
            EnvironmentResourceService environmentResourceService,
            EnvironmentEncryptionService environmentEncryptionService,
            AzureParametersRepository azureParametersRepository,
            DnsV1Endpoint dnsV1Endpoint,
            ProxyConfigService proxyConfigService,
            ProxyConfigModificationService proxyConfigModificationService,
            EnvironmentReactorFlowManager environmentReactorFlowManager,
            EnvironmentTagsDtoConverter environmentTagsDtoConverter,
            EnvironmentValidatorService environmentValidatorService,
            EventSenderService eventSenderService,
            FreeIpaService freeIpaService) {
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentService = environmentService;
        this.credentialService = credentialService;
        this.networkService = networkService;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.parametersService = parametersService;
        this.environmentFlowValidatorService = environmentFlowValidatorService;
        this.environmentResourceService = environmentResourceService;
        this.environmentEncryptionService = environmentEncryptionService;
        this.azureParametersRepository = azureParametersRepository;
        this.dnsV1Endpoint = dnsV1Endpoint;
        this.freeIpaService = freeIpaService;
        this.proxyConfigService = proxyConfigService;
        this.proxyConfigModificationService = proxyConfigModificationService;
        this.environmentReactorFlowManager = environmentReactorFlowManager;
        this.environmentTagsDtoConverter = environmentTagsDtoConverter;
        this.environmentValidatorService = environmentValidatorService;
        this.eventSenderService = eventSenderService;
    }

    public EnvironmentDto edit(Environment environment, EnvironmentEditDto editDto) {
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        editDescriptionIfChanged(environment, editDto);
        editTelemetryIfChanged(environment, editDto);
        editNetworkIfChanged(environment, editDto);
        editAdminGroupNameIfChanged(environment, editDto);
        editAuthenticationIfChanged(editDto, environment);
        editSecurityAccessIfChanged(editDto, environment);
        editHybridIfChanged(editDto, environment);
        editIdBrokerMappingSource(editDto, environment);
        editCloudStorageValidation(editDto, environment);
        editTunnelIfChanged(editDto, environment);
        editEnvironmentParameters(editDto, environment);
        editFreeIPA(editDto, environment);
        editDataServices(editDto, environment);
        editFreeIpaNodeCount(editDto, environment);
        editTags(editDto, environment);
        validationBuilder.merge(environmentValidatorService.validateTags(editDto));

        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }

        Environment saved = environmentService.save(environment);
        triggerEditProxyIfChanged(editDto, saved);

        return environmentDtoConverter.environmentToDto(saved);
    }

    public EnvironmentDto changeCredentialByEnvironmentName(String accountId, String environmentName, EnvironmentChangeCredentialDto dto) {
        Environment environment = getEnvironment(accountId, NameOrCrn.ofName(environmentName));
        return changeCredential(accountId, environmentName, dto, environment);
    }

    public EnvironmentDto changeCredentialByEnvironmentCrn(String accountId, String crn, EnvironmentChangeCredentialDto dto) {
        Environment environment = getEnvironment(accountId, NameOrCrn.ofCrn(crn));
        return changeCredential(accountId, crn, dto, environment);
    }

    public EnvironmentDto updateAzureResourceEncryptionParametersByEnvironmentName(String accountId, String environmentName,
            UpdateAzureResourceEncryptionDto dto) {
        Environment environment = getEnvironment(accountId, NameOrCrn.ofName(environmentName));
        return updateAzureResourceEncryptionParameters(accountId, environmentName, dto.getAzureResourceEncryptionParametersDto(), environment);
    }

    public EnvironmentDto updateAzureResourceEncryptionParametersByEnvironmentCrn(String accountId, String crn, UpdateAzureResourceEncryptionDto dto) {
        Environment environment = getEnvironment(accountId, NameOrCrn.ofCrn(crn));
        return updateAzureResourceEncryptionParameters(accountId, crn, dto.getAzureResourceEncryptionParametersDto(), environment);
    }

    public EnvironmentDto changeTelemetryFeaturesByEnvironmentName(String accountId, String environmentName,
            EnvironmentFeatures features) {
        Environment environment = getEnvironment(accountId, NameOrCrn.ofName(environmentName));
        return changeTelemetryFeatures(features, environment);
    }

    public EnvironmentDto changeTelemetryFeaturesByEnvironmentCrn(String accountId, String crn,
            EnvironmentFeatures features) {
        Environment environment = getEnvironment(accountId, NameOrCrn.ofCrn(crn));
        return changeTelemetryFeatures(features, environment);
    }

    public FlowIdentifier setupCrossRealmSetup(String accountId, NameOrCrn nameOrCrn, EnvironmentSetupCrossRealmTrustRequest request) {
        Environment environment = getEnvironment(accountId, nameOrCrn);
        return environmentReactorFlowManager.triggerSetupCrossRealmTrust(
                environment.getId(),
                environment.getAccountId(),
                environment.getName(),
                environment.getCreator(),
                environment.getResourceCrn(),
                request);
    }

    public FlowIdentifier setupFinishCrossRealmSetup(String accountId, NameOrCrn nameOrCrn) {
        Environment environment = getEnvironment(accountId, nameOrCrn);
        return environmentReactorFlowManager.triggerSetupFinishCrossRealmTrust(
                environment.getId(),
                environment.getName(),
                environment.getCreator(),
                environment.getResourceCrn());
    }

    public Environment getEnvironment(String accountId, NameOrCrn nameOrCrn) {
        if (nameOrCrn.hasCrn()) {
            return getEnvironmentByCrn(accountId, nameOrCrn.getCrn());
        } else {
            return getEnvironmentByName(accountId, nameOrCrn.getName());
        }
    }

    private void editTags(EnvironmentEditDto editDto, Environment environment) {
        if (MapUtils.isNotEmpty(editDto.getUserDefinedTags())) {
            environment.setTags(environmentTagsDtoConverter.getTags(editDto));
        }
    }

    private EnvironmentDto changeCredential(String accountId, String environmentName, EnvironmentChangeCredentialDto dto, Environment environment) {
        Credential credential = credentialService.getByNameForAccountId(dto.getCredentialName(), accountId, ENVIRONMENT);
        environment.setCredential(credential);
        LOGGER.debug("About to change credential on environment \"{}\"", environmentName);
        Environment saved = environmentService.save(environment);
        return environmentDtoConverter.environmentToDto(saved);
    }

    private EnvironmentDto updateAzureResourceEncryptionParameters(String accountId, String environmentName, AzureResourceEncryptionParametersDto dto,
            Environment environment) {
        AzureParameters azureParameters = (AzureParameters) environment.getParameters();
        if (dto.getEncryptionKeyUrl() != null) {
            if (azureParameters.getEncryptionKeyUrl() == null ||
                    (dto.getEncryptionKeyUrl().equals(azureParameters.getEncryptionKeyUrl()) && dto.getUserManagedIdentity() != null)) {
                ValidationResult validationResult = environmentService.getValidatorService().validateEncryptionKeyUrl(dto.getEncryptionKeyUrl());
                if (dto.getUserManagedIdentity() != null) {
                    validationResult = validationResult.merge(environmentService.getValidatorService().validateEncryptionRole(dto.getUserManagedIdentity()));
                }
                if (!validationResult.hasError()) {
                    azureParameters.setEncryptionKeyUrl(dto.getEncryptionKeyUrl());
                    azureParameters.setEncryptionKeyResourceGroupName(dto.getEncryptionKeyResourceGroupName());
                    azureParameters.setEnableHostEncryption(dto.getEnableHostEncryption());
                    azureParameters.setUserManagedIdentity(dto.getUserManagedIdentity());
                    //creating the DES
                    try {
                        EnvironmentDto environmentDto = environmentDtoConverter.environmentToDto(environment);
                        environmentEncryptionService.validateEncryptionParameters(environmentDto);
                        CreatedDiskEncryptionSet createdDiskEncryptionSet = environmentEncryptionService.createEncryptionResources(environmentDto);
                        azureParameters.setDiskEncryptionSetId(createdDiskEncryptionSet.getDiskEncryptionSetId());
                        azureParametersRepository.save(azureParameters);
                    } catch (Exception e) {
                        throw new BadRequestException(e);
                    }
                    LOGGER.debug("Successfully created the Disk encryption set for the environment {}.", environmentName);
                } else {
                    throw new BadRequestException(validationResult.getFormattedErrors());
                }
            } else if (azureParameters.getEncryptionKeyUrl().equals(dto.getEncryptionKeyUrl())) {
                LOGGER.info("Encryption Key '{}' is already set for the environment '{}'.", azureParameters.getEncryptionKeyUrl(), environmentName);
            } else {
                throw new BadRequestException(String.format("Encryption Key '%s' is already set for the environment '%s'. " +
                        "Modifying the encryption key is not allowed.", azureParameters.getEncryptionKeyUrl(), environmentName));
            }
        }
        return environmentDtoConverter.environmentToDto(environment);
    }

    private EnvironmentDto changeTelemetryFeatures(EnvironmentFeatures features, Environment environment) {
        EnvironmentTelemetry telemetry = environment.getTelemetry();
        if (telemetry != null) {
            EnvironmentFeatures actualFeatures = telemetry.getFeatures();
            if (actualFeatures != null) {
                if (features.getWorkloadAnalytics() != null) {
                    actualFeatures.setWorkloadAnalytics(features.getWorkloadAnalytics());
                    LOGGER.debug("Updating workload analytics (environment telemetry feature): {}.",
                            features.getWorkloadAnalytics().getEnabled());
                }
                if (features.getCloudStorageLogging() != null) {
                    actualFeatures.setCloudStorageLogging(features.getCloudStorageLogging());
                    LOGGER.debug("Updating cloud storage logging (environment telemetry feature): {}.",
                            features.getCloudStorageLogging().getEnabled());
                }
                telemetry.setFeatures(actualFeatures);
                // required to re-set as telemetry is saved as a JSON string
                environment.setTelemetry(telemetry);
            } else if (features != null) {
                telemetry.setFeatures(features);
                // required to re-set as telemetry is saved as a JSON string
                environment.setTelemetry(telemetry);
                LOGGER.debug("Adding new feature object for environment level telemetry.");
            } else {
                LOGGER.debug("No telemetry feature related changes has been made.");
            }
        }
        Environment saved = environmentService.save(environment);
        return environmentDtoConverter.environmentToDto(saved);
    }

    private void editNetworkIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (networkChanged(editDto)) {
            BaseNetwork network = networkService.validate(environment.getNetwork(), editDto, environment);
            network = networkService.refreshMetadataFromCloudProvider(network, editDto, environment);
            network = networkService.refreshProviderSpecificParameters(network, editDto, environment);
            network = networkService.refreshServiceEndpointCreation(network, editDto, environment);
            if (network != null) {
                environment.setNetwork(network);
            }
        }
    }

    private boolean networkChanged(EnvironmentEditDto editDto) {
        return editDto.getNetworkDto() != null;
    }

    private void editDescriptionIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (StringUtils.isNotEmpty(editDto.getDescription())) {
            environment.setDescription(editDto.getDescription());
        }
    }

    @VisibleForTesting
    void editAuthenticationIfChanged(EnvironmentEditDto editDto, Environment environment) {
        AuthenticationDto authenticationDto = editDto.getAuthentication();
        if (authenticationDto != null) {
            EnvironmentValidatorService validatorService = environmentService.getValidatorService();
            ValidationResult validationResult = validatorService.validateAuthenticationModification(editDto, environment);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            EnvironmentAuthentication originalAuthentication = environment.getAuthentication();
            if (environmentResourceService.isRawSshKeyUpdateSupported(environment)) {
                EnvironmentAuthentication updated = authenticationDtoConverter.dtoToSshUpdatedAuthentication(authenticationDto);
                updated.setLoginUserName(originalAuthentication.getLoginUserName());
                updated.setId(originalAuthentication.getId());
                environment.setAuthentication(updated);
            } else if (environmentResourceService.isExistingSshKeyUpdateSupported(environment)) {
                environment.setAuthentication(authenticationDtoConverter.dtoToAuthentication(authenticationDto));
                boolean cleanupOldSshKey = true;
                if (StringUtils.isNotEmpty(authenticationDto.getPublicKey())) {
                    cleanupOldSshKey = environmentResourceService.createAndUpdateSshKey(environment);
                }
                if (cleanupOldSshKey) {
                    String oldSshKeyId = originalAuthentication.getPublicKeyId();
                    LOGGER.info("The '{}' of ssh key is replaced with {}", oldSshKeyId, environment.getAuthentication().getPublicKeyId());
                    if (originalAuthentication.isManagedKey()) {
                        LOGGER.warn("Environment {} has managed public key. Skipping key deletion: '{}'", environment.getName(), oldSshKeyId);
                        EnvironmentDto environmentDto = environmentDtoConverter.environmentToDto(environment);
                        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
                        eventSenderService.sendEventAndNotification(environmentDto, userCrn, ResourceEvent.ENVIRONMENT_SSH_DELETION_SKIPPED,
                                List.of(oldSshKeyId));
                    }
                } else {
                    LOGGER.info("Authentication modification was unsuccessful. The authentication was reverted to the previous version.");
                    environment.setAuthentication(originalAuthentication);
                }
            }
        }
    }

    private void editTunnelIfChanged(EnvironmentEditDto editDto, Environment environment) {
        if (editDto.getTunnel() != null) {
            ExperimentalFeatures experimentalFeaturesJson = environment.getExperimentalFeaturesJson();
            experimentalFeaturesJson.setTunnel(editDto.getTunnel());
            environment.setExperimentalFeaturesJson(experimentalFeaturesJson);
        }
    }

    private void editSecurityAccessIfChanged(EnvironmentEditDto editDto, Environment environment) {
        SecurityAccessDto securityAccessDto = editDto.getSecurityAccess();
        if (securityAccessDto != null) {
            EnvironmentValidatorService validatorService = environmentService.getValidatorService();
            ValidationResult validationResult = validatorService.validateSecurityAccessModification(securityAccessDto, environment);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            validationResult = validatorService.validateSecurityGroups(editDto, environment);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            environmentService.editSecurityAccess(environment, securityAccessDto);
        }
    }

    private void editHybridIfChanged(EnvironmentEditDto editDto, Environment environment) {
        EnvironmentHybridDto environmentHybridDto = editDto.getEnvironmentHybridDto();
        if (environmentHybridDto != null) {
            environmentService.editHybrid(environment, environmentHybridDto);
        }
    }

    private void editTelemetryIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (editDto.getTelemetry() != null) {
            ValidationResult validationResult = environmentFlowValidatorService.validateTelemetryLoggingStorageLocation(environment);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            environment.setTelemetry(editDto.getTelemetry());
        }
    }

    private void editIdBrokerMappingSource(EnvironmentEditDto editDto, Environment environment) {
        IdBrokerMappingSource idBrokerMappingSource = editDto.getIdBrokerMappingSource();
        if (idBrokerMappingSource != null) {
            ExperimentalFeatures experimentalFeaturesJson = environment.getExperimentalFeaturesJson();
            experimentalFeaturesJson.setIdBrokerMappingSource(idBrokerMappingSource);
            environment.setExperimentalFeaturesJson(experimentalFeaturesJson);
        }
    }

    private void editCloudStorageValidation(EnvironmentEditDto editDto, Environment environment) {
        CloudStorageValidation cloudStorageValidation = editDto.getCloudStorageValidation();
        if (cloudStorageValidation != null) {
            ExperimentalFeatures experimentalFeaturesJson = environment.getExperimentalFeaturesJson();
            experimentalFeaturesJson.setCloudStorageValidation(cloudStorageValidation);
            environment.setExperimentalFeaturesJson(experimentalFeaturesJson);
        }
    }

    private void editAdminGroupNameIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (editDto.getAdminGroupName() != null) {
            environmentService.setAdminGroupName(environment, editDto.getAdminGroupName());
        }
    }

    private boolean shouldSendSubnetIdsToFreeIpa(AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest) {
        AddDnsZoneNetwork addDnsZoneNetwork = addDnsZoneForSubnetIdsRequest.getAddDnsZoneNetwork();
        return StringUtils.isNotBlank(addDnsZoneNetwork.getNetworkId())
                && addDnsZoneNetwork.getSubnetIds() != null
                && !addDnsZoneNetwork.getSubnetIds().isEmpty();
    }

    private void editFreeIPA(EnvironmentEditDto editDto, Environment environment) {
        NetworkDto networkDto = environmentDtoConverter.networkToNetworkDto(environment);
        AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = addDnsZoneForSubnetIdsRequest(editDto, environment, networkDto);
        Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(environment.getResourceCrn());
        if (freeIpaResponse.isPresent() && shouldSendSubnetIdsToFreeIpa(addDnsZoneForSubnetIdsRequest)) {
            dnsV1Endpoint.addDnsZoneForSubnetIds(addDnsZoneForSubnetIdsRequest);
        }
    }

    private void triggerEditProxyIfChanged(EnvironmentEditDto editDto, Environment env) {
        if (editDto.getProxyConfig() != null && proxyConfigModificationService.shouldModify(env, editDto.getProxyConfig())) {
            ProxyConfig newProxyConfig = editDto.getProxyConfig().getName() == null
                    ? null
                    : proxyConfigService.getByNameForAccountId(editDto.getProxyConfig().getName(), editDto.getAccountId());
            EnvironmentDto environmentDto = environmentDtoConverter.environmentToDto(env);
            proxyConfigModificationService.validateModify(environmentDto);
            environmentReactorFlowManager.triggerEnvironmentProxyConfigModification(environmentDto, newProxyConfig);
        }
    }

    private AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest(EnvironmentEditDto environmentEditDto,
            Environment environment, NetworkDto networkDto) {
        AddDnsZoneForSubnetIdsRequest addDnsZoneForSubnetIdsRequest = new AddDnsZoneForSubnetIdsRequest();
        addDnsZoneForSubnetIdsRequest.setEnvironmentCrn(environment.getResourceCrn());
        AddDnsZoneNetwork addDnsZoneNetwork = new AddDnsZoneNetwork();
        if (networkDto != null) {
            addDnsZoneNetwork.setNetworkId(networkDto.getNetworkId());
        }
        if (environmentEditDto.getNetworkDto() != null) {
            addDnsZoneNetwork.setSubnetIds(environmentEditDto.getNetworkDto().getSubnetIds());
        }
        addDnsZoneForSubnetIdsRequest.setAddDnsZoneNetwork(addDnsZoneNetwork);
        return addDnsZoneForSubnetIdsRequest;
    }

    private void editEnvironmentParameters(EnvironmentEditDto editDto, Environment environment) {
        ParametersDto parametersDto = editDto.getParameters();
        if (parametersDto != null) {
            Optional<BaseParameters> original = parametersService.findByEnvironment(environment.getId());
            if (original.isPresent()) {
                BaseParameters originalParameters = original.get();
                parametersDto.setId(originalParameters.getId());
                if (originalParameters instanceof AwsParameters awsOriginalParameters) {
                    parametersDto.getAwsParametersDto().setFreeIpaSpotPercentage(awsOriginalParameters.getFreeIpaSpotPercentage());
                    validateAwsParameters(environment, parametersDto);
                }
            }
            if (parametersDto.getGcpParametersDto() != null) {
                String encryptionKey = Optional.of(parametersDto.getGcpParametersDto())
                        .map(GcpParametersDto::getGcpResourceEncryptionParametersDto)
                        .map(GcpResourceEncryptionParametersDto::getEncryptionKey)
                        .orElse(null);
                ValidationResult validationResult = environmentService.getValidatorService().validateEncryptionKey(encryptionKey);
                if (validationResult.hasError()) {
                    throw new BadRequestException(validationResult.getFormattedErrors());
                }
            }
            if (parametersDto.azureParametersDto() != null && parametersDto.getAzureParametersDto().getAzureResourceEncryptionParametersDto() != null) {
                UpdateAzureResourceEncryptionDto updateAzureResourceEncryptionDto = UpdateAzureResourceEncryptionDto.builder()
                        .withAzureResourceEncryptionParametersDto(editDto.getParameters().getAzureParametersDto().getAzureResourceEncryptionParametersDto())
                        .build();
                updateAzureResourceEncryptionParametersByEnvironmentName(
                        environment.getAccountId(),
                        environment.getName(),
                        updateAzureResourceEncryptionDto);
            }
            BaseParameters parameters = parametersService.saveParameters(environment, parametersDto);
            if (parameters != null) {
                environment.setParameters(parameters);
            }
        }
    }

    private void validateAwsParameters(Environment environment, ParametersDto parametersDto) {
        if (parametersDto.getAwsParametersDto() != null) {
            EnvironmentValidationDto environmentValidationDto = getEnvironmentValidationDto(environment);
            ValidationResult validationResult = environmentFlowValidatorService
                    .validateParameters(environmentValidationDto, parametersDto);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }

    private Environment getEnvironmentByName(String accountId, String environmentName) {
        return environmentService.findByNameAndAccountIdAndArchivedIsFalse(environmentName, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("No environment found with name '%s'", environmentName)));
    }

    private Environment getEnvironmentByCrn(String accountId, String environmentCrn) {
        return environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(environmentCrn, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("No environment found with crn '%s'", environmentCrn)));
    }

    private void editDataServices(EnvironmentEditDto editDto, Environment environment) {
        EnvironmentDataServices dataServices = editDto.getDataServices();
        if (dataServices != null) {
            environment.setDataServices(dataServices);
            EnvironmentValidationDto environmentValidationDto = getEnvironmentValidationDto(environment);
            ValidationResult validationResult = environmentFlowValidatorService.validateEnvironmentDataServices(environmentValidationDto);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }

    private void editFreeIpaNodeCount(EnvironmentEditDto editDto, Environment environment) {
        Integer freeipaNodeCount = editDto.getFreeipaNodeCount();
        if (freeipaNodeCount != null) {
            environment.setFreeIpaInstanceCountByGroup(freeipaNodeCount);
            EnvironmentValidationDto environmentValidationDto = getEnvironmentValidationDto(environment);
            ValidationResult validationResult = environmentFlowValidatorService.validateEnvironmentDataServices(environmentValidationDto);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }

    private EnvironmentValidationDto getEnvironmentValidationDto(Environment environment) {
        EnvironmentDto environmentDto = environmentDtoConverter.environmentToDto(environment);
        return EnvironmentValidationDto.builder()
                .withEnvironmentDto(environmentDto)
                .withValidationType(ValidationType.ENVIRONMENT_EDIT)
                .build();
    }
}