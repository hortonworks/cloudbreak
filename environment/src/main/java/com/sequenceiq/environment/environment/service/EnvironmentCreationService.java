package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.CloudPlatform.AWS;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.NetworkService;
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

    private final NetworkService networkService;

    public EnvironmentCreationService(
            EnvironmentService environmentService,
            EnvironmentValidatorService validatorService,
            EnvironmentResourceService environmentResourceService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentReactorFlowManager reactorFlowManager,
            AuthenticationDtoConverter authenticationDtoConverter,
            ParametersService parametersService,
            NetworkService networkService) {
        this.environmentService = environmentService;
        this.validatorService = validatorService;
        this.environmentResourceService = environmentResourceService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.reactorFlowManager = reactorFlowManager;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.parametersService = parametersService;
        this.networkService = networkService;
    }

    //TODO: accountId is kind of duplicated - creationDto also has accountId. should be removed?
    public EnvironmentDto create(EnvironmentCreationDto creationDto, String accountId, String creator) {
        if (environmentService.isNameOccupied(creationDto.getName(), creationDto.getAccountId())) {
            throw new BadRequestException(String.format("Environment with name '%s' already exists in account '%s'.",
                    creationDto.getName(), creationDto.getAccountId()));
        }
        Environment environment = initializeEnvironment(creationDto, creator);
        environmentService.setSecurityAccess(environment, creationDto.getSecurityAccess());
        environmentService.setAdminGroupName(environment, creationDto.getAdminGroupName());
        CloudRegions cloudRegions = setLocationAndRegions(creationDto, environment);
        validateCreation(creator, creationDto, environment, cloudRegions);
        Map<String, CloudSubnet> subnetMetas = networkService.retrieveSubnetMetadata(environment, creationDto.getNetwork());
        validateNetworkRequest(environment, creationDto.getNetwork(), subnetMetas);
        environment = environmentService.save(environment);
        environmentResourceService.createAndSetNetwork(environment, creationDto.getNetwork(), accountId, subnetMetas);
        createAndSetParameters(environment, creationDto.getParameters(), accountId);
        environmentService.save(environment);
        reactorFlowManager.triggerCreationFlow(environment.getId(), environment.getName(), creator, environment.getResourceCrn());
        return environmentDtoConverter.environmentToDto(environment);
    }

    private void validateNetworkRequest(Environment environment, NetworkDto network, Map<String, CloudSubnet> subnetMetas) {
        ValidationResult.ValidationResultBuilder errors = new ValidationResult.ValidationResultBuilder();
        String message;
        if (AWS.name().equalsIgnoreCase(environment.getCloudPlatform()) && network != null && network.getNetworkCidr() == null) {
            if (network.getSubnetIds().size() < 2) {
                message = "Cannot create environment, there should be at least two subnets in the network";
                LOGGER.info(message);
                errors.error(message);
            }
            if (subnetMetas.size() != network.getSubnetIds().size()) {
                message = String.format("Subnets of the environment (%s) are not found in the vpc (%s). ",
                        String.join(",", getSubnetDiff(network.getSubnetIds(), subnetMetas.keySet())),
                        networkService.getAwsVpcId(network).orElse(""));
                LOGGER.info(message);
                errors.error(message);
            }
            Map<String, Long> zones = subnetMetas.values().stream()
                    .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
            if (zones.size() < 2) {
                message = String.format("Cannot create environment, the subnets in the vpc should be at least in two different availabilityzones");
                LOGGER.debug(message);
                errors.error(message);
            }
        }
        ValidationResult validationResult = errors.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private Set<String> getSubnetDiff(Set<String> envSubnets, Set<String> vpcSubnets) {
        Set<String> diff = new HashSet<>();
        for (String envSubnet : envSubnets) {
            if (!vpcSubnets.contains(envSubnet)) {
                diff.add(envSubnet);
            }
        }
        return diff;
    }

    private Environment initializeEnvironment(EnvironmentCreationDto creationDto, String creator) {
        Environment environment = environmentDtoConverter.creationDtoToEnvironment(creationDto);
        environment.setResourceCrn(createCrn(creationDto.getAccountId()));
        environment.setCreator(creator);
        environment.setCreated(System.currentTimeMillis());
        Credential credential = environmentResourceService
                .getCredentialFromRequest(creationDto.getCredential(), creationDto.getAccountId(), creator);
        environment.setCredential(credential);
        environment.setCloudPlatform(credential.getCloudPlatform());
        environment.setTunnel(creationDto.getTunnel());
        environment.setAuthentication(authenticationDtoConverter.dtoToAuthentication(creationDto.getAuthentication()));
        return environment;
    }

    private CloudRegions setLocationAndRegions(EnvironmentCreationDto creationDto, Environment environment) {
        CloudRegions cloudRegions = environmentService.getRegionsByEnvironment(environment);
        environmentService.setLocation(environment, creationDto.getLocation(), cloudRegions);
        if (cloudRegions.areRegionsSupported()) {
            environmentService.setRegions(environment, creationDto.getRegions(), cloudRegions);
        }
        return cloudRegions;
    }

    private void validateCreation(String userCrn, EnvironmentCreationDto creationDto, Environment environment, CloudRegions cloudRegions) {
        ValidationResult validationResult = validatorService.validateCreation(environment, creationDto, cloudRegions);
        validationResult = validationResult.merge(validatorService.validateTelemetryLoggingStorageLocation(userCrn, environment));
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void createAndSetParameters(Environment environment, ParametersDto parameters, String accountId) {
        environment.setParameters(parametersService.saveParameters(environment, parameters, accountId));
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
}
