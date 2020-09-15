package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Optional;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
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

    public EnvironmentCreationService(
            EnvironmentService environmentService,
            EnvironmentValidatorService environmentValidatorService,
            EnvironmentResourceService environmentResourceService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentReactorFlowManager reactorFlowManager,
            AuthenticationDtoConverter authenticationDtoConverter,
            ParametersService parametersService,
            EntitlementService entitlementService) {
        this.environmentService = environmentService;
        validatorService = environmentValidatorService;
        this.environmentResourceService = environmentResourceService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.reactorFlowManager = reactorFlowManager;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.parametersService = parametersService;
        this.entitlementService = entitlementService;
    }

    public EnvironmentDto create(EnvironmentCreationDto creationDto) {
        LOGGER.info("Environment creation initiated.");
        if (environmentService.isNameOccupied(creationDto.getName(), creationDto.getAccountId())) {
            throw new BadRequestException(String.format("Environment with name '%s' already exists in account '%s'.",
                    creationDto.getName(), creationDto.getAccountId()));
        }

        Environment environment = initializeEnvironment(creationDto);
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
                    getIfNotNull(creationDto.getNetwork(), NetworkDto::getSubnetMetas));
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
        LOGGER.info("Environment is initialized for creation.");
        return environment;
    }

    private void createAndSetParameters(Environment environment, ParametersDto parameters) {
        environment.setParameters(parametersService.saveParameters(environment, parameters));
    }

    private void validateCreation(EnvironmentCreationDto creationDto, Environment environment) {
        LOGGER.info("Validating environment creation. CreationDto: '{}', Environment: '{}'.", creationDto, environment);
        ValidationResultBuilder validationBuilder = validatorService.validateNetworkCreation(environment, creationDto.getNetwork());
        ValidationResult parentChildValidation = validatorService.validateParentChildRelation(environment, creationDto.getParentEnvironmentName());
        validationBuilder.merge(parentChildValidation);
        validationBuilder.ifError(() -> isCloudPlatformInvalid(creationDto.getCreator(), creationDto.getCloudPlatform()),
                "Provisioning in Microsoft Azure is not enabled for this account.");
        ValidationResult freeIpaCreationValidation = validatorService.validateFreeIpaCreation(creationDto.getFreeIpaCreation());
        validationBuilder.merge(freeIpaCreationValidation);
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private boolean isCloudPlatformInvalid(String userCrn, String cloudPlatform) {
        return (AZURE.name().equalsIgnoreCase(cloudPlatform) && !entitlementService.azureEnabled(userCrn, Crn.safeFromString(userCrn).getAccountId()))
                || (GCP.name().equalsIgnoreCase(cloudPlatform) && !entitlementService.gcpEnabled(userCrn, Crn.safeFromString(userCrn).getAccountId()));
    }
}
