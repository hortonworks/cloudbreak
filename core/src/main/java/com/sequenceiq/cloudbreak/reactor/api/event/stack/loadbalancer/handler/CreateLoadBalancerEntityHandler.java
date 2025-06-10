package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateLoadBalancerEntityFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateLoadBalancerEntityRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateLoadBalancerEntitySuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CreateLoadBalancerEntityHandler extends ExceptionCatcherEventHandler<CreateLoadBalancerEntityRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateLoadBalancerEntityHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private NetworkService networkService;

    @Inject
    private SubnetSelector subnetSelector;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CreateLoadBalancerEntityRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CreateLoadBalancerEntityRequest> event) {
        return new CreateLoadBalancerEntityFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CreateLoadBalancerEntityRequest> event) {
        CreateLoadBalancerEntityRequest request = event.getData();
        Stack stack = stackService.getById(request.getResourceId());
        try {
            LOGGER.debug("Creating load balancer entity objects for stack {}", stack.getId());
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());

            if (environment != null && environment.getNetwork() != null &&
                    (PublicEndpointAccessGateway.ENABLED.equals(environment.getNetwork().getPublicEndpointAccessGateway()) ||
                            isTargetingEndpointGateway(environment))) {
                enableEndpointGateway(stack, environment);
            }

            Set<InstanceGroup> instanceGroups = instanceGroupService.getByStackAndFetchTemplates(stack.getId());
            Set<LoadBalancer> existingLoadBalancers = loadBalancerPersistenceService.findByStackId(stack.getId());
            stack.setInstanceGroups(instanceGroups);
            Set<LoadBalancer> newLoadBalancers = loadBalancerConfigService.createLoadBalancers(stack, environment, null);

            if (doLoadBalancersAlreadyExist(existingLoadBalancers, newLoadBalancers)) {
                LOGGER.debug("Load balancer entities already exist. Continuing flow.");
            } else {
                LOGGER.debug("Persisting stack and load balancer objects to database.");
                stack.setLoadBalancers(newLoadBalancers);
                String stackName = stack.getName();
                measure(() -> stackService.save(stack),
                    LOGGER, "Stackrepository save took {} ms for stack {}", stackName);
                measure(() -> loadBalancerPersistenceService.saveAll(newLoadBalancers),
                    LOGGER, "Load balancers saved in {} ms for stack {}", stackName);
                measure(() -> targetGroupPersistenceService.saveAll(newLoadBalancers.stream()
                        .flatMap(lb -> lb.getTargetGroupSet().stream()).collect(Collectors.toSet())),
                    LOGGER, "Target groups saved in {} ms for stack {}", stackName);
                measure(() -> instanceGroupService.saveAll(stack.getInstanceGroups()),
                    LOGGER, "Instance groups saved in {} ms for stack {}", stackName);
                if (stack.getNetwork() != null) {
                    measure(() -> networkService.pureSave(stack.getNetwork()), LOGGER,
                        "Network saved in {} ms for stack {}", stackName);
                }
            }

            LOGGER.debug("Load balancer entities successfully persisted.");
            return new CreateLoadBalancerEntitySuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.warn("Failed create load balancer entities and persist them to the database.", e);
            return new CreateLoadBalancerEntityFailure(request.getResourceId(), e);
        }
    }

    private boolean isTargetingEndpointGateway(DetailedEnvironmentResponse environment) {
        return entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(environment.getAccountId()) &&
                CollectionUtils.isNotEmpty(environment.getNetwork().getEndpointGatewaySubnetIds());
    }

    private void enableEndpointGateway(Stack stack, DetailedEnvironmentResponse environment) throws CloudbreakException {
        if (stack.getNetwork() == null) {
            throw new CloudbreakException("Could not create endpoint gateway; network information is missing from stack");
        }

        Json attributes = stack.getNetwork().getAttributes();
        Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
        String subnetId = params.get(SUBNET_ID) != null ? String.valueOf(params.get(SUBNET_ID)) : null;
        Optional<CloudSubnet> endpointGatewaySubnet = subnetSelector.chooseSubnetForEndpointGateway(environment.getNetwork(), subnetId);
        if (endpointGatewaySubnet.isEmpty()) {
            throw new CloudbreakException("Could not find subnet to create endpoint gateway load balancer");
        }
        params.put(ENDPOINT_GATEWAY_SUBNET_ID, endpointGatewaySubnet.get().getId());
        stack.getNetwork().setAttributes(new Json(params));
    }

    private boolean doLoadBalancersAlreadyExist(Set<LoadBalancer> existingLoadBalancers, Set<LoadBalancer> newLoadBalancers) {
        return existingLoadBalancers.stream().map(LoadBalancer::getType).collect(Collectors.toSet()).equals(
            newLoadBalancers.stream().map(LoadBalancer::getType).collect(Collectors.toSet()));
    }
}
