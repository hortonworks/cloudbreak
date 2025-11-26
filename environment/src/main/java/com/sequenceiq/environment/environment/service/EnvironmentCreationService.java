package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.service.recipe.EnvironmentRecipeService;
import com.sequenceiq.environment.environment.service.validation.SeLinuxValidationService;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.LoadBalancerEntitlementService;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
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

    private final LoadBalancerEntitlementService loadBalancerEntitlementService;

    private final EnvironmentRecipeService recipeService;

    private final OwnerAssignmentService ownerAssignmentService;

    private final EntitlementService entitlementService;

    private final SeLinuxValidationService seLinuxValidationService;

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
            LoadBalancerEntitlementService loadBalancerEntitlementService,
            EnvironmentRecipeService recipeService,
            OwnerAssignmentService ownerAssignmentService,
            EntitlementService entitlementService,
            SeLinuxValidationService seLinuxValidationService) {
        this.environmentService = environmentService;
        validatorService = environmentValidatorService;
        this.environmentResourceService = environmentResourceService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.reactorFlowManager = reactorFlowManager;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.parametersService = parametersService;
        this.loadBalancerEntitlementService = loadBalancerEntitlementService;
        this.recipeService = recipeService;
        this.ownerAssignmentService = ownerAssignmentService;
        this.entitlementService = entitlementService;
        this.seLinuxValidationService = seLinuxValidationService;
    }

    public EnvironmentDto create(EnvironmentCreationDto creationDto) {
        LOGGER.info("Environment creation initiated.");

        loadBalancerEntitlementService.validateNetworkForEndpointGateway(creationDto.getCloudPlatform(), creationDto.getName(),
                getIfNotNull(creationDto.getNetwork(), NetworkDto::getEndpointGatewaySubnetIds));

        if (environmentService.isNameOccupied(creationDto.getName(), creationDto.getAccountId())) {
            throw new BadRequestException(String.format("Environment with name '%s' already exists in account '%s'.",
                    creationDto.getName(), creationDto.getAccountId()));
        }

        ownerAssignmentService.assignResourceOwnerRoleIfEntitled(ThreadBasedUserCrnProvider.getUserCrn(), creationDto.getCrn());

        Environment environment = initializeEnvironment(creationDto);
        initializeEnvironmentTunnel(environment);
        if (StringUtils.isNotEmpty(creationDto.getParentEnvironmentName())) {
            LOGGER.debug("Setting parent environment '{}'.", creationDto.getParentEnvironmentName());
            Optional<Environment> parentEnvironment = environmentService.
                    findByNameAndAccountIdAndArchivedIsFalse(creationDto.getParentEnvironmentName(), creationDto.getAccountId());
            parentEnvironment.ifPresent(environment::setParentEnvironment);
        }

        environmentService.setSecurityAccess(environment, creationDto.getSecurityAccess());
        initializeSecretEncryption(environment);
        validateCreation(creationDto, environment);
        validateRecipes(creationDto);
        validateSecurity(creationDto);
        try {
            environment = environmentService.save(environment);
            saveFreeIpaRecipes(creationDto, environment);
            environmentResourceService.createAndSetNetwork(environment, creationDto.getNetwork(), creationDto.getAccountId(),
                    getIfNotNull(creationDto.getNetwork(), NetworkDto::getSubnetMetas),
                    getIfNotNull(creationDto.getNetwork(), NetworkDto::getEndpointGatewaySubnetMetas));
            createAndSetParameters(environment, creationDto.getParameters());
            environmentService.save(environment);
            reactorFlowManager.triggerCreationFlow(environment.getId(), environment.getName(), creationDto.getCreator(), environment.getResourceCrn());
        } catch (Exception e) {
            environment.setStatus(EnvironmentStatus.CREATE_FAILED);
            environment.setStatusReason(e.getMessage());
            environmentService.save(environment);
            throw e;
        }
        return environmentDtoConverter.environmentToDto(environment);
    }

    private void validateSecurity(EnvironmentCreationDto creationDto) {
        if (creationDto.getFreeIpaCreation() != null) {
            try {
                seLinuxValidationService.validateSeLinuxEntitlementGrantedForFreeipaCreation(creationDto.getFreeIpaCreation());
            } catch (CloudbreakServiceException e) {
                throw new BadRequestException(e);
            }
        }
    }

    private void validateRecipes(EnvironmentCreationDto creationDto) {
        if (creationDto.getFreeIpaCreation() != null && creationDto.getFreeIpaCreation().getRecipes() != null) {
            validatorService.validateFreeipaRecipesExistsByName(creationDto.getFreeIpaCreation().getRecipes());
        }
    }

    private void saveFreeIpaRecipes(EnvironmentCreationDto creationDto, Environment environment) {
        if (creationDto.getFreeIpaCreation() != null && creationDto.getFreeIpaCreation().getRecipes() != null) {
            recipeService.saveRecipes(creationDto.getFreeIpaCreation().getRecipes(), environment.getId());
        }
    }

    private Environment initializeEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = environmentDtoConverter.creationDtoToEnvironment(creationDto);
        environment.setResourceCrn(creationDto.getCrn());
        Credential credential = environmentResourceService.getCredentialFromRequest(creationDto.getCredential(), creationDto.getAccountId());
        environment.setCredential(credential);
        Optional<ProxyConfig> proxyConfig = environmentResourceService.getProxyConfig(creationDto.getProxyConfigName(), creationDto.getAccountId());
        proxyConfig.ifPresent(environment::setProxyConfig);
        environment.setCloudPlatform(credential.getCloudPlatform());
        environment.setAuthentication(authenticationDtoConverter.dtoToAuthentication(creationDto.getAuthentication()));
        environment.setEnvironmentServiceVersion(environmentServiceVersion);

        Optional<EncryptionProfile> encryptionProfileOp = environmentResourceService.getEncryptionProfile(creationDto.getEncryptionProfileCrn());
        encryptionProfileOp.ifPresent(encryptionProfile -> environment.setEncryptionProfileCrn(encryptionProfile.getResourceCrn()));
        LOGGER.info("Environment is initialized for creation.");
        return environment;
    }

    private void initializeSecretEncryption(Environment environment) {
        environment.setEnableSecretEncryption(CloudPlatform.AWS.name().equals(environment.getCloudPlatform()) &&
                Boolean.TRUE.equals(environment.getCredential().getGovCloud()) && entitlementService.isSecretEncryptionEnabled(environment.getAccountId()));
        LOGGER.info("Environment is initialized with enableSecretEncryption={}.", environment.isEnableSecretEncryption());
    }

    private void initializeEnvironmentTunnel(Environment environment) {
        ExperimentalFeatures experimentalFeatures = environment.getExperimentalFeaturesJson();
        Tunnel tunnel = experimentalFeatures.getTunnel();
        boolean overrideTunnel = experimentalFeatures.isOverrideTunnel();
        if (!overrideTunnel) {
            if (Tunnel.CCM == tunnel || Tunnel.CCMV2 == tunnel) {
                tunnel = Tunnel.CCMV2_JUMPGATE;
                experimentalFeatures.setTunnel(tunnel);
                environment.setExperimentalFeaturesJson(experimentalFeatures);
            }
        }
        LOGGER.info("Environment is initialized with [{}] tunnel.", tunnel);
    }

    private void createAndSetParameters(Environment environment, ParametersDto parameters) {
        environment.setParameters(parametersService.saveParameters(environment, parameters));
    }

    private void validateCreation(EnvironmentCreationDto creationDto, Environment environment) {
        LOGGER.info("Validating environment creation. CreationDto: '{}', Environment: '{}'.", creationDto, environment);
        ValidationResultBuilder validationBuilder = validatorService.validateNetworkCreation(environment, creationDto.getNetwork());
        validationBuilder.merge(validatorService.validatePublicKey(creationDto.getAuthentication().getPublicKey()));
        validationBuilder.merge(validatorService.validateTags(creationDto));
        if (creationDto.getCloudPlatform() != null) {
            validationBuilder.merge(validateEncryptionKey(creationDto, environment.isEnableSecretEncryption()));
            validationBuilder.merge(validateEncryptionRole(creationDto));
        }
        ValidationResult parentChildValidation = validatorService.validateParentChildRelation(environment, creationDto.getParentEnvironmentName());
        validationBuilder.merge(parentChildValidation);
        EnvironmentTelemetry environmentTelemetry = creationDto.getTelemetry();
        if (environmentTelemetry != null && environmentTelemetry.getLogging() != null && environmentTelemetry.getLogging().getStorageLocation() != null) {
            validationBuilder.merge(validatorService.validateStorageLocation(environmentTelemetry.getLogging().getStorageLocation(), "logging"));
        }
        ValidationResult freeIpaCreationValidation = validatorService.validateFreeIpaCreation(creationDto.getFreeIpaCreation(), creationDto.getAccountId());
        validationBuilder.merge(freeIpaCreationValidation);
        if (creationDto.getExternalizedComputeCluster() != null) {
            Set<String> environmentSubnets = Collections.emptySet();
            if (creationDto.getNetwork() != null) {
                environmentSubnets = creationDto.getNetwork().getSubnetMetas().keySet();
            }
            validationBuilder.merge(validatorService.validateExternalizedComputeCluster(creationDto.getExternalizedComputeCluster(),
                    creationDto.getAccountId(), environmentSubnets));
        }
        validationBuilder.merge(validateEnvironmentType(creationDto));
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private ValidationResult validateEncryptionKey(EnvironmentCreationDto creationDto, boolean secretEncryptionEnabled) {
        String cloudPlatform = creationDto.getCloudPlatform().toLowerCase(Locale.ROOT);
        switch (cloudPlatform) {
            case "azure" -> {
                String encryptionKeyUrl = Optional.ofNullable(creationDto.getParameters())
                        .map(ParametersDto::getAzureParametersDto)
                        .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                        .map(AzureResourceEncryptionParametersDto::getEncryptionKeyUrl)
                        .orElse(null);
                return encryptionKeyUrl != null ? validatorService.validateEncryptionKeyUrl(encryptionKeyUrl) : ValidationResult.empty();
            }
            case "gcp" -> {
                String encryptionKey = Optional.ofNullable(creationDto.getParameters())
                        .map(ParametersDto::getGcpParametersDto)
                        .map(GcpParametersDto::getGcpResourceEncryptionParametersDto)
                        .map(GcpResourceEncryptionParametersDto::getEncryptionKey)
                        .orElse(null);
                return encryptionKey != null ? validatorService.validateEncryptionKey(encryptionKey) : ValidationResult.empty();
            }
            case "aws" -> {
                String encryptionKeyArn = Optional.ofNullable(creationDto.getParameters())
                        .map(ParametersDto::getAwsParametersDto)
                        .map(AwsParametersDto::getAwsDiskEncryptionParametersDto)
                        .map(AwsDiskEncryptionParametersDto::getEncryptionKeyArn)
                        .orElse(null);
                return validatorService.validateEncryptionKeyArn(encryptionKeyArn, secretEncryptionEnabled);
            }
            default -> {
                return ValidationResult.empty();
            }
        }
    }

    private ValidationResult validateEncryptionRole(EnvironmentCreationDto creationDto) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        String cloudPlatform = creationDto.getCloudPlatform().toLowerCase(Locale.ROOT);
        switch (cloudPlatform) {
            case "azure":
                String encryptionRole = Optional.ofNullable(creationDto.getParameters())
                        .map(ParametersDto::getAzureParametersDto)
                        .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                        .map(AzureResourceEncryptionParametersDto::getUserManagedIdentity)
                        .orElse(null);
                if (encryptionRole != null) {
                    resultBuilder.merge(validatorService.validateEncryptionRole(encryptionRole));
                }
                break;
            default:
                break;
        }
        return resultBuilder.build();
    }

    private ValidationResult validateEnvironmentType(EnvironmentCreationDto creationDto) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if ((EnvironmentType.HYBRID.equals(creationDto.getEnvironmentType()) ||  EnvironmentType.HYBRID_BASE.equals(creationDto.getEnvironmentType()))
                && !entitlementService.hybridCloudEnabled(creationDto.getAccountId())) {
            resultBuilder.error("Creating Hybrid Environment requires CDP_HYBRID_CLOUD entitlement for your account");
        }
        return resultBuilder.build();
    }
}
