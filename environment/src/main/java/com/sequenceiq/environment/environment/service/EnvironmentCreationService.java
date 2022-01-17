package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Optional;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.LoadBalancerEntitlementService;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

@Service
public class EnvironmentCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCreationService.class);

    private final EnvironmentService environmentService;

    private final EnvironmentValidatorService validatorService;

    private final EnvironmentResourceService environmentResourceService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    private final ParametersService parametersService;

    private final EntitlementService entitlementService;

    private final LoadBalancerEntitlementService loadBalancerEntitlementService;

    @Value("${info.app.version}")
    private String environmentServiceVersion;

    public EnvironmentCreationService(
            EnvironmentService environmentService,
            EnvironmentValidatorService environmentValidatorService,
            EnvironmentResourceService environmentResourceService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentReactorFlowManager reactorFlowManager,
            AuthenticationDtoConverter authenticationDtoConverter,
            ParametersService parametersService,
            EntitlementService entitlementService,
            LoadBalancerEntitlementService loadBalancerEntitlementService) {
        this.environmentService = environmentService;
        validatorService = environmentValidatorService;
        this.environmentResourceService = environmentResourceService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.reactorFlowManager = reactorFlowManager;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.parametersService = parametersService;
        this.entitlementService = entitlementService;
        this.loadBalancerEntitlementService = loadBalancerEntitlementService;
    }

    public EnvironmentDto create(EnvironmentCreationDto creationDto) {
        LOGGER.info("Environment creation initiated.");

        PublicEndpointAccessGateway endpointAccessGateway = creationDto.getNetwork() == null ?
            null : creationDto.getNetwork().getPublicEndpointAccessGateway();
        loadBalancerEntitlementService.validateNetworkForEndpointGateway(creationDto.getCloudPlatform(), creationDto.getName(),
            endpointAccessGateway);

        if (environmentService.isNameOccupied(creationDto.getName(), creationDto.getAccountId())) {
            throw new BadRequestException(String.format("Environment with name '%s' already exists in account '%s'.",
                    creationDto.getName(), creationDto.getAccountId()));
        }

        Environment environment = initializeEnvironment(creationDto);
        initializeEnvironmentTunnel(environment);
        if (StringUtils.isNotEmpty(creationDto.getParentEnvironmentName())) {
            LOGGER.debug("Setting parent environment '{}'.", creationDto.getParentEnvironmentName());
            Optional<Environment> parentEnvironment = environmentService.
                    findByNameAndAccountIdAndArchivedIsFalse(creationDto.getParentEnvironmentName(), creationDto.getAccountId());
            parentEnvironment.ifPresent(environment::setParentEnvironment);
        }

        environmentService.setSecurityAccess(environment, creationDto.getSecurityAccess());
        validateCreation(creationDto, environment);
        try {
            environment = environmentService.save(environment);
            environmentResourceService.createAndSetNetwork(environment, creationDto.getNetwork(), creationDto.getAccountId(),
                    getIfNotNull(creationDto.getNetwork(), NetworkDto::getSubnetMetas),
                    getIfNotNull(creationDto.getNetwork(), NetworkDto::getEndpointGatewaySubnetMetas));
            createAndSetParameters(environment, creationDto.getParameters());
            environmentService.saveWithOwnerRoleAssignment(environment);
            reactorFlowManager.triggerCreationFlow(environment.getId(), environment.getName(), creationDto.getCreator(), environment.getResourceCrn());
        } catch (Exception e) {
            environment.setStatus(EnvironmentStatus.CREATE_FAILED);
            environment.setStatusReason(e.getMessage());
            environmentService.save(environment);
            throw e;
        }
        return environmentDtoConverter.environmentToDto(environment);
    }

    private Environment initializeEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = environmentDtoConverter.creationDtoToEnvironment(creationDto);
        environment.setResourceCrn(creationDto.getCrn());
        Credential credential = environmentResourceService.getCredentialFromRequest(creationDto.getCredential(), creationDto.getAccountId());
        environment.setCredential(credential);
        Optional<ProxyConfig> proxyConfig = environmentResourceService.getProxyConfig(creationDto.getProxyConfigName(), creationDto.getAccountId());
        proxyConfig.ifPresent(pc -> environment.setProxyConfig(pc));
        environment.setCloudPlatform(credential.getCloudPlatform());
        environment.setAuthentication(authenticationDtoConverter.dtoToAuthentication(creationDto.getAuthentication()));
        environment.setEnvironmentServiceVersion(environmentServiceVersion);
        LOGGER.info("Environment is initialized for creation.");
        return environment;
    }

    private void initializeEnvironmentTunnel(Environment environment) {
        ExperimentalFeatures experimentalFeatures = environment.getExperimentalFeaturesJson();
        Tunnel tunnel = experimentalFeatures.getTunnel();
        boolean overrideTunnel = experimentalFeatures.isOverrideTunnel();
        boolean ccmV2Enabled = entitlementService.ccmV2Enabled(environment.getAccountId());
        boolean ccmV2JumpgateEnabled = entitlementService.ccmV2JumpgateEnabled(environment.getAccountId());
        checkCcmEntitlements(tunnel, ccmV2Enabled, ccmV2JumpgateEnabled);
        if (!overrideTunnel) {
            if (Tunnel.CCM == tunnel) {
                if (ccmV2JumpgateEnabled) {
                    tunnel = Tunnel.CCMV2_JUMPGATE;
                } else if (ccmV2Enabled) {
                    tunnel = Tunnel.CCMV2;
                }
                experimentalFeatures.setTunnel(tunnel);
                environment.setExperimentalFeaturesJson(experimentalFeatures);
            }
            // TODO: if we want the Jumpgate to be default then this logic needs to be added for CCMV2 as well
        }
        LOGGER.info("Environment is initialized with [{}] tunnel.", tunnel);
    }

    private void checkCcmEntitlements(Tunnel tunnel, boolean ccmV2Enabled, boolean ccmV2JumpgateEnabled) {
        if (Tunnel.CCMV2_JUMPGATE == tunnel && !ccmV2JumpgateEnabled) {
            throw new BadRequestException("CCMV2 Jumpgate not enabled for account.");
        }
        if (Tunnel.CCMV2 == tunnel && !ccmV2Enabled) {
            throw new BadRequestException("CCMV2 not enabled for account.");
        }
    }

    private void createAndSetParameters(Environment environment, ParametersDto parameters) {
        environment.setParameters(parametersService.saveParameters(environment, parameters));
    }

    private void validateCreation(EnvironmentCreationDto creationDto, Environment environment) {
        LOGGER.info("Validating environment creation. CreationDto: '{}', Environment: '{}'.", creationDto, environment);
        ValidationResultBuilder validationBuilder = validatorService.validateNetworkCreation(environment, creationDto.getNetwork());
        validationBuilder.merge(validatorService.validatePublicKey(creationDto.getAuthentication().getPublicKey()));
        validationBuilder.merge(validatorService.validateTags(creationDto));
        if (AZURE.name().equalsIgnoreCase(creationDto.getCloudPlatform())) {
            String encryptionKeyUrl = Optional.ofNullable(creationDto.getParameters())
                    .map(paramsDto -> paramsDto.getAzureParametersDto())
                    .map(azureParamsDto -> azureParamsDto.getAzureResourceEncryptionParametersDto())
                    .map(azureREParamsDto -> azureREParamsDto.getEncryptionKeyUrl()).orElse(null);
            if (encryptionKeyUrl != null) {
                validationBuilder.merge(validatorService.validateEncryptionKeyUrl(encryptionKeyUrl, creationDto.getAccountId()));
            }
        }
        if (GCP.name().equalsIgnoreCase(creationDto.getCloudPlatform())) {
            validationBuilder.merge(validatorService.validateEncryptionKey(creationDto));
        }
        if (AWS.name().equalsIgnoreCase(creationDto.getCloudPlatform())) {
            String encryptionKeyArn = Optional.ofNullable(creationDto.getParameters())
                    .map(paramsDto -> paramsDto.getAwsParametersDto())
                    .map(awsParamsDto -> awsParamsDto.getAwsDiskEncryptionParametersDto())
                    .map(awsREparamsDto -> awsREparamsDto.getEncryptionKeyArn()).orElse(null);
            if (encryptionKeyArn != null) {
                validationBuilder.merge(validatorService.validateEncryptionKeyArn(encryptionKeyArn, creationDto.getAccountId()));
            }
        }

        ValidationResult parentChildValidation = validatorService.validateParentChildRelation(environment, creationDto.getParentEnvironmentName());
        validationBuilder.merge(parentChildValidation);
        EnvironmentTelemetry environmentTelemetry = creationDto.getTelemetry();
        if (environmentTelemetry != null && environmentTelemetry.getLogging() != null && environmentTelemetry.getLogging().getStorageLocation() != null) {
            validationBuilder.merge(validatorService.validateStorageLocation(environmentTelemetry.getLogging().getStorageLocation(), "logging"));
        }
        validationBuilder.ifError(() -> isCloudPlatformInvalid(creationDto.getCreator(), creationDto.getCloudPlatform()),
                "Provisioning in " + creationDto.getCloudPlatform() + " is not enabled for this account.");
        ValidationResult freeIpaCreationValidation = validatorService.validateFreeIpaCreation(creationDto.getFreeIpaCreation());
        validationBuilder.merge(freeIpaCreationValidation);
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private boolean isCloudPlatformInvalid(String userCrn, String cloudPlatform) {
        return (AZURE.name().equalsIgnoreCase(cloudPlatform) && !entitlementService.azureEnabled(Crn.safeFromString(userCrn).getAccountId()))
                || (GCP.name().equalsIgnoreCase(cloudPlatform) && !entitlementService.gcpEnabled(Crn.safeFromString(userCrn).getAccountId()));
    }
}