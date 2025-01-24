package com.sequenceiq.environment.environment.flow.loadbalancer.handler;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateFailedEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateHandlerSelectors;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.network.NetworkMetadataValidationService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class LoadBalancerEnvUpdateHandler extends EventSenderAwareHandler<EnvironmentLoadBalancerDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerEnvUpdateHandler.class);

    private final NetworkMetadataValidationService networkValidationService;

    private final NetworkService networkService;

    private final EnvironmentService environmentService;

    private final EntitlementService entitlementService;

    protected LoadBalancerEnvUpdateHandler(
            EventSender eventSender,
            NetworkMetadataValidationService networkValidationService,
            NetworkService networkService,
            EnvironmentService environmentService,
            EntitlementService entitlementService) {
        super(eventSender);
        this.networkValidationService = networkValidationService;
        this.networkService = networkService;
        this.environmentService = environmentService;
        this.entitlementService = entitlementService;
    }

    @Override
    public String selector() {
        return LoadBalancerUpdateHandlerSelectors.ENVIRONMENT_UPDATE_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentLoadBalancerDto> envLoadBalancerDtoEvent) {
        EnvironmentLoadBalancerDto environmentLoadBalancerDto = envLoadBalancerDtoEvent.getData();
        EnvironmentDto environmentDto = environmentLoadBalancerDto.getEnvironmentDto();
        requireNonNull(environmentDto);
        requireNonNull(environmentDto.getNetwork());

        try {
            Environment environment = environmentService.findEnvironmentByIdOrThrow(environmentDto.getResourceId());
            requireNonNull(environment);
            requireNonNull(environment.getNetwork());

            LOGGER.debug("Starting endpoint gateway update for environment {}", environmentDto.getResourceCrn());
            NetworkDto networkDto = environmentDto.getNetwork();

            Set<String> endpointGatewaySubnetIds = environmentLoadBalancerDto.getEndpointGatewaySubnetIds();
            if (PublicEndpointAccessGateway.ENABLED.equals(environmentLoadBalancerDto.getEndpointAccessGateway()) ||
                    isTargetingEndpointGateway(environment.getAccountId(), endpointGatewaySubnetIds)) {
                requireNonNull(endpointGatewaySubnetIds);
                LOGGER.debug("Enabling endpoint gateway on environment network with public IP {}.", environmentLoadBalancerDto.getEndpointAccessGateway());
                networkDto.setPublicEndpointAccessGateway(environmentLoadBalancerDto.getEndpointAccessGateway());
                Map<String, CloudSubnet> tempSubnetMetas = endpointGatewaySubnetIds.stream()
                    .collect(toMap(id -> id, id ->
                            new CloudSubnet.Builder()
                                    .id(id)
                                    .build()
                    ));
                networkDto.setEndpointGatewaySubnetMetas(tempSubnetMetas);

                LOGGER.debug("Fetching metadata for endpoint gateway subnets.");
                Map<String, CloudSubnet> endpointGatewaySubnetMetas =
                    networkValidationService.getEndpointGatewaySubnetMetadata(environment, environmentDto);

                LOGGER.debug("Updating environment network settings.");
                environment.getNetwork().setPublicEndpointAccessGateway(environmentLoadBalancerDto.getEndpointAccessGateway());
                environment.getNetwork().setEndpointGatewaySubnetMetas(endpointGatewaySubnetMetas);

                LOGGER.debug("Persisting updated network to database.");
                networkService.save(environment.getNetwork());
            }

            LOGGER.debug("Environment network load balancer update complete.");
            LoadBalancerUpdateEvent loadBalancerUpdateEvent = LoadBalancerUpdateEvent.Builder.aLoadBalancerUpdateEvent()
                .withSelector(LoadBalancerUpdateStateSelectors.LOAD_BALANCER_STACK_UPDATE_EVENT.selector())
                .withResourceId(environment.getId())
                .withResourceName(environment.getResourceName())
                .withResourceCrn(environment.getResourceCrn())
                .withEnvironmentDto(environmentDto)
                .withEnvironment(environment)
                .withEndpointAccessGateway(environmentLoadBalancerDto.getEndpointAccessGateway())
                .withSubnetIds(endpointGatewaySubnetIds)
                .build();
            eventSender().sendEvent(loadBalancerUpdateEvent, envLoadBalancerDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.error("Caught exception while updating environment state with laod balancers.", e);
            LoadBalancerUpdateFailedEvent failedEvent = new LoadBalancerUpdateFailedEvent(environmentDto, e,
                EnvironmentStatus.LOAD_BALANCER_ENV_UPDATE_FAILED);
            eventSender().sendEvent(failedEvent, envLoadBalancerDtoEvent.getHeaders());
        }
    }

    private boolean isTargetingEndpointGateway(String accountId, Set<String> endpointGatewaySubnetIds) {
        return entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(accountId) &&
                CollectionUtils.isNotEmpty(endpointGatewaySubnetIds);
    }
}
