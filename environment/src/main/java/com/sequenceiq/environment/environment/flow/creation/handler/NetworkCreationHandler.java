package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.YARN;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_PUBLICKEY_CREATION_EVENT;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class NetworkCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkCreationHandler.class);

    private static final String UNMATCHED_AZ = "Please provide public subnets in each of the following availability zones: %s";

    private final EnvironmentService environmentService;

    private final NetworkService networkService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final EnvironmentResourceService environmentResourceService;

    private final CloudNetworkService cloudNetworkService;

    private final Set<String> enabledPlatforms;

    private final EventBus eventBus;

    private final EntitlementService entitlementService;

    protected NetworkCreationHandler(EventSender eventSender,
            EnvironmentService environmentService,
            NetworkService networkService,
            EnvironmentNetworkService environmentNetworkService,
            CloudNetworkService cloudNetworkService,
            EnvironmentResourceService environmentResourceService,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms,
            EventBus eventBus,
            EntitlementService entitlementService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.networkService = networkService;
        this.environmentNetworkService = environmentNetworkService;
        this.enabledPlatforms = enabledPlatforms;
        this.cloudNetworkService = cloudNetworkService;
        this.environmentResourceService = environmentResourceService;
        this.eventBus = eventBus;
        this.entitlementService = entitlementService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId())
                    .ifPresent(environment -> {
                        setChildEnvironmentNetworkIfItHasParentWithTheSameCloudProvider(environmentDto);
                        Map<String, CloudSubnet> subnetMetas = null;
                        Map<String, CloudSubnet> endpointGatewaySubnetMetas = null;
                        if (environmentDto.getNetwork() != null) {
                            LOGGER.debug("Environment ({}) dto has network, hence we're filling it's related subnet fields", environment.getName());
                            subnetMetas = cloudNetworkService.retrieveSubnetMetadata(environmentDto, environmentDto.getNetwork());
                            environmentDto.getNetwork().setSubnetMetas(subnetMetas);
                            if (entitlementService.publicEndpointAccessGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
                                endpointGatewaySubnetMetas = removePrivateSubnets(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(
                                    environmentDto, environmentDto.getNetwork()));
                            } else {
                                environmentDto.getNetwork().setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);
                            }
                            environmentDto.getNetwork().setEndpointGatewaySubnetMetas(endpointGatewaySubnetMetas);
                            environmentResourceService.createAndSetNetwork(environment, environmentDto.getNetwork(), environment.getAccountId(),
                                    environmentDto.getNetwork().getSubnetMetas(), environmentDto.getNetwork().getEndpointGatewaySubnetMetas());
                        } else {
                            LOGGER.debug("Environment ({}) dto has no network!", environment.getName());
                        }
                        validateSubnetsIfProvided(environment, subnetMetas, endpointGatewaySubnetMetas);
                        createCloudNetworkIfNeeded(environmentDto, environment);
                        createProviderSpecificNetworkResourcesIfNeeded(environmentDto, environment.getNetwork());
                        environmentService.save(environment);
                    });
            initiateNextStep(environmentDtoEvent, environmentDto);
        } catch (Exception e) {
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                    environmentDto.getId(),
                    environmentDto.getName(),
                    e,
                    environmentDto.getResourceCrn());
            eventBus.notify(failureEvent.selector(), new Event<>(environmentDtoEvent.getHeaders(), failureEvent));
        }
    }

    private void setChildEnvironmentNetworkIfItHasParentWithTheSameCloudProvider(EnvironmentDto currentEnvDto) {
        String parentEnvName = currentEnvDto.getParentEnvironmentName();
        if (StringUtils.isNotEmpty(parentEnvName) && currentEnvDto.getCloudPlatform() != null &&
                currentEnvDto.getCloudPlatform().equals(currentEnvDto.getParentEnvironmentCloudPlatform())) {
            LOGGER.debug("Parent environment (with name: {} ) has detected, going to fetch it for it's network for the child.", parentEnvName);
            EnvironmentDto parentEnvDto = environmentService.getByNameAndAccountId(parentEnvName, currentEnvDto.getAccountId());
            NetworkDto parentNetworkDto = parentEnvDto.getNetwork();
            parentNetworkDto.setId(currentEnvDto.getNetwork().getId());
            currentEnvDto.setNetwork(parentNetworkDto);
        }
    }

    private Map<String, CloudSubnet> removePrivateSubnets(Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        LOGGER.debug("Removing any private subnets from the provided endpoint gateway list because they won't be used.");
        if (endpointGatewaySubnetMetas == null || endpointGatewaySubnetMetas.isEmpty()) {
            return Map.of();
        }
        return endpointGatewaySubnetMetas.entrySet().stream()
            .filter(entry -> !entry.getValue().isPrivateSubnet())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void validateSubnetsIfProvided(Environment environment, Map<String, CloudSubnet> subnetMetas,
            Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        if (shouldValidateEndpointGatewaySubnets(environment, subnetMetas)) {
            if (endpointGatewaySubnetMetas != null && !endpointGatewaySubnetMetas.isEmpty()) {
                valildateProvidedEndpointGatewaySubnets(subnetMetas, endpointGatewaySubnetMetas);
            } else {
                valildateEnvironmentSubnetsForEndpointGateway(subnetMetas);
            }
        }
    }

    private boolean shouldValidateEndpointGatewaySubnets(Environment environment, Map<String, CloudSubnet> subnetMetas) {
        return entitlementService.publicEndpointAccessGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId()) &&
            hasNetwork(environment) && environment.getNetwork().getPublicEndpointAccessGateway() == PublicEndpointAccessGateway.ENABLED &&
            environment.getNetwork().getRegistrationType() != RegistrationType.CREATE_NEW && subnetMetas != null && !subnetMetas.isEmpty();
    }

    private void valildateProvidedEndpointGatewaySubnets(Map<String, CloudSubnet> subnetMetas, Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        LOGGER.debug("Verifying that provided endpoint gateway subnets share availability zones with provided environment subnets.");
        Set<String> subnetAZs = subnetMetas.values().stream()
            .map(CloudSubnet::getAvailabilityZone)
            .collect(Collectors.toSet());
        Set<String> endpointGatewaySubnetAZs = endpointGatewaySubnetMetas.values().stream()
            .map(CloudSubnet::getAvailabilityZone)
            .collect(Collectors.toSet());
        if (!subnetAZs.equals(endpointGatewaySubnetAZs)) {
            throw new BadRequestException(String.format(UNMATCHED_AZ, subnetAZs));
        }
    }

    private void valildateEnvironmentSubnetsForEndpointGateway(Map<String, CloudSubnet> subnetMetas) {
        LOGGER.debug("Verifying that public subnets in availability zones that match the private subnets were provided.");
        Set<String> privateAZs = subnetMetas.values().stream()
            .filter(CloudSubnet::isPrivateSubnet)
            .map(CloudSubnet::getAvailabilityZone)
            .collect(Collectors.toSet());
        Set<String> publicAZs = subnetMetas.values().stream()
            .filter(subnet -> !subnet.isPrivateSubnet())
            .map(CloudSubnet::getAvailabilityZone)
            .collect(Collectors.toSet());
        if (!privateAZs.equals(publicAZs)) {
            throw new BadRequestException(String.format(UNMATCHED_AZ, privateAZs));
        }
    }

    private void createCloudNetworkIfNeeded(EnvironmentDto environmentDto, Environment environment) {
        if (hasNetwork(environment) && environment.getNetwork().getRegistrationType() == RegistrationType.CREATE_NEW) {
            BaseNetwork baseNetwork = environmentNetworkService.createCloudNetwork(environmentDto, environment.getNetwork());
            baseNetwork = networkService.save(baseNetwork);
            environment.setNetwork(baseNetwork);
        }
    }

    private void createProviderSpecificNetworkResourcesIfNeeded(EnvironmentDto environmentDto, BaseNetwork network) {
        if (AZURE.equalsIgnoreCase(environmentDto.getCloudPlatform())) {
            environmentNetworkService.createProviderSpecificNetworkResources(environmentDto, network);
        }
    }

    private boolean hasNetwork(Environment environment) {
        return Objects.nonNull(environment.getNetwork())
                && !YARN.equalsIgnoreCase(environment.getCloudPlatform())
                && !MOCK.equalsIgnoreCase(environment.getCloudPlatform())
                && enabledPlatforms.contains(environment.getCloudPlatform());
    }

    private void initiateNextStep(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto) {
        EnvCreationEvent envCreationEvent = EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(START_PUBLICKEY_CREATION_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }

    @Override
    public String selector() {
        return CREATE_NETWORK_EVENT.selector();
    }
}
