package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.CloudPlatform.AZURE;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.base.Tunnel;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

@Service
public class EnvironmentCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCreationService.class);

    private static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    private final EnvironmentService environmentService;

    private final EnvironmentValidatorService validatorService;

    private final EnvironmentResourceService environmentResourceService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    private final ParametersService parametersService;

    private final NetworkService networkService;

    private final EntitlementService entitlementService;

    private final GrpcUmsClient grpcUmsClient;

    public EnvironmentCreationService(
            EnvironmentService environmentService,
            EnvironmentValidatorService environmentValidatorService,
            EnvironmentResourceService environmentResourceService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentReactorFlowManager reactorFlowManager,
            AuthenticationDtoConverter authenticationDtoConverter,
            ParametersService parametersService,
            NetworkService networkService,
            EntitlementService entitlementService,
            GrpcUmsClient grpcUmsClient) {
        this.environmentService = environmentService;
        this.validatorService = environmentValidatorService;
        this.environmentResourceService = environmentResourceService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.reactorFlowManager = reactorFlowManager;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.parametersService = parametersService;
        this.networkService = networkService;
        this.entitlementService = entitlementService;
        this.grpcUmsClient = grpcUmsClient;
    }

    public EnvironmentDto create(EnvironmentCreationDto creationDto) {
        if (environmentService.isNameOccupied(creationDto.getName(), creationDto.getAccountId())) {
            throw new BadRequestException(String.format("Environment with name '%s' already exists in account '%s'.",
                    creationDto.getName(), creationDto.getAccountId()));
        }
        Environment environment = initializeEnvironment(creationDto);
        environmentService.setSecurityAccess(environment, creationDto.getSecurityAccess());
        String workloadAdministrationGroupName = createAdminGroup(creationDto, environment.getResourceCrn());
        environmentService.setAdminGroupName(environment, workloadAdministrationGroupName);
        CloudRegions cloudRegions = setLocationAndRegions(creationDto, environment);
        Map<String, CloudSubnet> subnetMetas = networkService.retrieveSubnetMetadata(environment, creationDto.getNetwork());
        validateCreation(creationDto, environment, cloudRegions, subnetMetas);
        environment = environmentService.save(environment);
        environmentResourceService.createAndSetNetwork(environment, creationDto.getNetwork(), creationDto.getAccountId(), subnetMetas);
        createAndSetParameters(environment, creationDto.getParameters());
        environmentService.save(environment);
        reactorFlowManager.triggerCreationFlow(environment.getId(), environment.getName(), creationDto.getCreator(), environment.getResourceCrn());
        return environmentDtoConverter.environmentToDto(environment);
    }

    private Environment initializeEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = environmentDtoConverter.creationDtoToEnvironment(creationDto);
        environment.setResourceCrn(createCrn(creationDto.getAccountId()));
        Credential credential = environmentResourceService
                .getCredentialFromRequest(creationDto.getCredential(), creationDto.getAccountId(), creationDto.getCreator());
        environment.setCredential(credential);
        environment.setCloudPlatform(credential.getCloudPlatform());
        environment.setAuthentication(authenticationDtoConverter.dtoToAuthentication(creationDto.getAuthentication()));
        return environment;
    }

    private String createAdminGroup(EnvironmentCreationDto creationDto, String envCrn) {
        String workloadAdministrationGroupName = null;
        if (StringUtils.isEmpty(creationDto.getAdminGroupName())) {
            workloadAdministrationGroupName = grpcUmsClient.setWorkloadAdministrationGroupName(IAM_INTERNAL_ACTOR_CRN, creationDto.getAccountId(),
                    Optional.empty(), "environments/adminClouderaManager", envCrn);
            LOGGER.info("Created workloadAdministrationGroupName: {}", workloadAdministrationGroupName);
        } else {
            // To keep backward compatibility, if somebody passes the group name, then we shall just use it
            workloadAdministrationGroupName = creationDto.getAdminGroupName();
            LOGGER.info("User has defined aadmin group: {}", workloadAdministrationGroupName);
        }
        return workloadAdministrationGroupName;
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

    private String createCrn(@Nonnull String accountId) {
        return Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    private void validateCreation(EnvironmentCreationDto creationDto, Environment environment, CloudRegions cloudRegions, Map<String, CloudSubnet> subnetMetas) {
        ValidationResultBuilder validationBuilder = validatorService
                .validateRegionsAndLocation(creationDto.getLocation().getName(), creationDto.getRegions(), environment, cloudRegions);
        ValidationResultBuilder networkValidation = validatorService.validateNetworkCreation(environment, creationDto.getNetwork(), subnetMetas);
        validationBuilder = validationBuilder.merge(networkValidation.build());

        validationBuilder.ifError(() -> isTunnelInvalid(creationDto.getCreator(), creationDto.getExperimentalFeatures().getTunnel()),
                "Reverse SSH tunnel is not enabled for this account.");

        validationBuilder.ifError(() -> isCloudPlatformInvalid(creationDto.getCreator(), creationDto.getCloudPlatform()),
                "Provisioning in Microsoft Azure is not enabled for this account.");

        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private boolean isTunnelInvalid(String userCrn, Tunnel tunnel) {
        return !entitlementService.ccmEnabled(userCrn, Crn.safeFromString(userCrn).getAccountId()) && Tunnel.CCM.equals(tunnel);
    }

    private boolean isCloudPlatformInvalid(String userCrn, String cloudPlatform) {
        return AZURE.name().equalsIgnoreCase(cloudPlatform) && !entitlementService.azureEnabled(userCrn, Crn.safeFromString(userCrn).getAccountId());
    }
}
