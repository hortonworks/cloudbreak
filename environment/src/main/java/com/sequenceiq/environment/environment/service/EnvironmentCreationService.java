package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
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

    private final VirtualGroupService virtualGroupService;

    public EnvironmentCreationService(
            EnvironmentService environmentService,
            EnvironmentValidatorService environmentValidatorService,
            EnvironmentResourceService environmentResourceService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentReactorFlowManager reactorFlowManager,
            AuthenticationDtoConverter authenticationDtoConverter,
            ParametersService parametersService,
            EntitlementService entitlementService,
            VirtualGroupService virtualGroupService) {
        this.environmentService = environmentService;
        validatorService = environmentValidatorService;
        this.environmentResourceService = environmentResourceService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.reactorFlowManager = reactorFlowManager;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.parametersService = parametersService;
        this.entitlementService = entitlementService;
        this.virtualGroupService = virtualGroupService;
    }

    public EnvironmentDto create(EnvironmentCreationDto creationDto) {
        if (environmentService.isNameOccupied(creationDto.getName(), creationDto.getAccountId())) {
            throw new BadRequestException(String.format("Environment with name '%s' already exists in account '%s'.",
                    creationDto.getName(), creationDto.getAccountId()));
        }

        Environment environment = initializeEnvironment(creationDto);
        if (Objects.nonNull(creationDto.getParentEnvironmentName())) {
            Optional<Environment> parentEnvironment = environmentService.
                    findByNameAndAccountIdAndArchivedIsFalse(creationDto.getParentEnvironmentName(), creationDto.getAccountId());
            parentEnvironment.ifPresent(environment::setParentEnvironment);
        }

        environmentService.setSecurityAccess(environment, creationDto.getSecurityAccess());
        String environmentCrnForVirtualGroups = getEnvironmentCrnForVirtualGroups(environment);
        if (!createVirtualGroups(creationDto, environmentCrnForVirtualGroups)) {
            // To keep backward compatibility, if somebody passes the group name, then we shall just use it
            environmentService.setAdminGroupName(environment, creationDto.getAdminGroupName());
        }
        environmentService.assignEnvironmentAdminRole(creationDto.getCreator(), environmentCrnForVirtualGroups);

        CloudRegions cloudRegions = setLocationAndRegions(creationDto, environment);
        validateCreation(creationDto, environment, cloudRegions);
        try {
            environment = environmentService.save(environment);
            environmentResourceService.createAndSetNetwork(environment, creationDto.getNetwork(), creationDto.getAccountId(),
                    getIfNotNull(creationDto.getNetwork(), NetworkDto::getSubnetMetas));
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

    private Environment initializeEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = environmentDtoConverter.creationDtoToEnvironment(creationDto);
        environment.setResourceCrn(creationDto.getCrn());
        Credential credential = environmentResourceService
                .getCredentialFromRequest(creationDto.getCredential(), creationDto.getAccountId(), creationDto.getCreator());
        environment.setCredential(credential);
        environment.setCloudPlatform(credential.getCloudPlatform());
        environment.setAuthentication(authenticationDtoConverter.dtoToAuthentication(creationDto.getAuthentication()));
        return environment;
    }

    private String getEnvironmentCrnForVirtualGroups(Environment environment) {
        String environmentCrnForVirtualGroups = environment.getResourceCrn();
        if (Objects.nonNull(environment.getParentEnvironment())) {
            environmentCrnForVirtualGroups = environment.getParentEnvironment().getResourceCrn();
        }
        return environmentCrnForVirtualGroups;
    }

    private boolean createVirtualGroups(EnvironmentCreationDto creationDto, String envCrn) {
        boolean result = false;
        if (StringUtils.isEmpty(creationDto.getAdminGroupName())) {
            Map<UmsRight, String> virtualGroups = virtualGroupService.createVirtualGroups(creationDto.getAccountId(), envCrn);
            LOGGER.info("The virtualgroups for [{}] environment are created: {}", creationDto.getName(), virtualGroups);
            result = true;
        }
        return result;
    }

    private CloudRegions setLocationAndRegions(EnvironmentCreationDto creationDto, Environment environment) {
        CloudRegions cloudRegions = environmentService.getRegionsByEnvironment(environment);
        environmentService.setLocation(environment, creationDto.getLocation(), cloudRegions);
        if (cloudRegions.areRegionsSupported()) {
            environmentService.setRegions(environment, creationDto.getRegions(), cloudRegions);
        }
        return cloudRegions;
    }

    private void createAndSetParameters(Environment environment, ParametersDto parameters) {
        environment.setParameters(parametersService.saveParameters(environment, parameters));
    }

    private void validateCreation(EnvironmentCreationDto creationDto, Environment environment, CloudRegions cloudRegions) {
        ValidationResultBuilder validationBuilder = validatorService
                .validateRegionsAndLocation(creationDto.getLocation().getName(), creationDto.getRegions(), environment, cloudRegions);
        ValidationResultBuilder networkValidation = validatorService.validateNetworkCreation(environment, creationDto.getNetwork());
        validationBuilder = validationBuilder.merge(networkValidation.build());

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
        return AZURE.name().equalsIgnoreCase(cloudPlatform) && !entitlementService.azureEnabled(userCrn, Crn.safeFromString(userCrn).getAccountId());
    }
}
