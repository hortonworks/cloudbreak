package com.sequenceiq.cloudbreak.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.TargetGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Service
public class LoadBalancerConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerConfigService.class);

    private static final String ENDPOINT_SUFFIX = "gateway";

    private static final Set<Integer> DEFAULT_KNOX_PORTS = Set.of(443);

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private EntitlementService entitlementService;

    public Set<String> getKnoxGatewayGroups(Stack stack) {
        LOGGER.debug("Fetching list of instance groups with Knox gateway installed");
        Set<String> groupNames = new HashSet<>();
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            LOGGER.debug("Checking if Knox gateway is explicitly defined");
            CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(cluster.getBlueprint().getBlueprintText());
            groupNames = cmTemplateProcessor.getHostGroupsWithComponent(KnoxRoles.KNOX_GATEWAY);
        }

        if (groupNames.isEmpty()) {
            LOGGER.debug("Knox gateway is not explicitly defined; searching for CM gateway hosts");
            groupNames = stack.getInstanceGroups().stream()
                .filter(i -> InstanceGroupType.isGateway(i.getInstanceGroupType()))
                .map(InstanceGroup::getGroupName)
                .collect(Collectors.toSet());
        }

        if (groupNames.isEmpty()) {
            LOGGER.info("No Knox gateway instance groups found");
        }
        return groupNames;
    }

    private boolean isKnoxGatewayDefinedInServices(Set<ServiceComponent> serviceComponents) {
        return serviceComponents.stream()
            .anyMatch(serviceComponent -> KnoxRoles.KNOX_GATEWAY.equals(serviceComponent.getComponent()));
    }

    public String generateLoadBalancerEndpoint(Stack stack) {
        StringBuilder name = new StringBuilder()
            .append(stack.getName())
            .append('-')
            .append(ENDPOINT_SUFFIX);
        return name.toString();
    }

    public Set<Integer> getPortsForTargetGroup(TargetGroup targetGroup) {
        switch (targetGroup.getType()) {
            case KNOX:
                return DEFAULT_KNOX_PORTS;
            default:
                return Collections.emptySet();
        }
    }

    public String getLoadBalancerUserFacingFQDN(Long stackId) {
        Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stackId);
        if (!loadBalancers.isEmpty()) {
            LoadBalancer preferredLB = loadBalancers.stream().filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType())).findAny()
                .orElse(loadBalancers.iterator().next());
            return preferredLB.getFqdn();
        }

        return null;
    }

    public Optional<LoadBalancer> selectLoadBalancer(Set<LoadBalancer> loadBalancers, LoadBalancerType preferredType) {
        Preconditions.checkNotNull(preferredType);
        Optional<LoadBalancer> loadBalancerOptional = loadBalancers.stream()
            .filter(lb -> preferredType.equals(lb.getType()))
            .findFirst();
        if (loadBalancerOptional.isPresent()) {
            LOGGER.debug("Found load balancer of type {}", preferredType);
        } else {
            loadBalancerOptional = loadBalancers.stream()
                .filter(lb -> lb.getType() != null && !preferredType.equals(lb.getType()))
                .findFirst();
            if (loadBalancerOptional.isPresent()) {
                LOGGER.debug("Could not find load balancer of preferred type {}. Using type {}", preferredType, loadBalancerOptional.get().getType());
            }
        }

        if (loadBalancerOptional.isEmpty()) {
            LOGGER.debug("Unable to find load balancer");
        }
        return loadBalancerOptional;
    }

    public Set<LoadBalancer> createLoadBalancers(Stack stack, DetailedEnvironmentResponse environment) {
        LOGGER.info("Setting up load balancers for stack {}", stack.getDisplayName());
        Set<LoadBalancer> loadBalancers = new HashSet<>();

        if (isLoadBalancerEnabled(stack.getType(), environment)) {
            LOGGER.debug("Load balancers are enabled for data lake and data hub stacks.");
            Optional<TargetGroup> knoxTargetGroup = setupKnoxTargetGroup(stack);
            if (knoxTargetGroup.isPresent()) {
                if (arePrivateSubnetsAvailable(environment.getNetwork())) {
                    LOGGER.debug("Found Knox enabled instance groups in stack. Setting up internal Knox load balancer");
                    setupKnoxLoadBalancer(
                        createLoadBalancerIfNotExists(loadBalancers, LoadBalancerType.PRIVATE, stack),
                        knoxTargetGroup.get());
                }
                if (shouldCreateExternalKnoxLoadBalancer(environment.getNetwork())) {
                    LOGGER.debug("Public endpoint access gateway is enabled. Setting up public Knox load balancer");
                    setupKnoxLoadBalancer(
                        createLoadBalancerIfNotExists(loadBalancers, LoadBalancerType.PUBLIC, stack),
                        knoxTargetGroup.get());
                }
            } else {
                LOGGER.debug("No Knox instance groups found. If load balancer creation is enabled, Knox routing in the load balancer will be skipped.");
            }

            // TODO CB-9368 - create target group for CM instances
        }

        return loadBalancers;
    }

    private boolean isLoadBalancerEnabled(StackType type, DetailedEnvironmentResponse environment) {
        return isLoadBalancerEnabledForDatalake(type, environment) || isLoadBalancerEnabledForDatahub(type, environment);
    }

    private boolean isLoadBalancerEnabledForDatalake(StackType type, DetailedEnvironmentResponse environment) {
        return StackType.DATALAKE.equals(type) && environment != null &&
            (entitlementService.datalakeLoadBalancerEnabled(ThreadBasedUserCrnProvider.getAccountId()) ||
                isEndpointGatewayEnabled(environment.getNetwork()));
    }

    private boolean isLoadBalancerEnabledForDatahub(StackType type, DetailedEnvironmentResponse environment) {
        return StackType.WORKLOAD.equals(type) && environment != null && isEndpointGatewayEnabled(environment.getNetwork());
    }

    private boolean arePrivateSubnetsAvailable(EnvironmentNetworkResponse network) {
        LOGGER.debug("Checking if network has private subnets defined...");
        boolean result = network != null && network.getSubnetMetas() != null &&
            network.getSubnetMetas().values().stream().anyMatch(CloudSubnet::isPrivateSubnet);
        if (result) {
            LOGGER.debug("Private subnets found.");
        } else {
            LOGGER.debug("No private subnets found. Internal load balancer for stack will not be created.");
        }
        return result;
    }

    private boolean shouldCreateExternalKnoxLoadBalancer(EnvironmentNetworkResponse network) {
        return isEndpointGatewayEnabled(network) || !arePrivateSubnetsAvailable(network);
    }

    private boolean isEndpointGatewayEnabled(EnvironmentNetworkResponse network) {
        return network != null && network.getPublicEndpointAccessGateway() == PublicEndpointAccessGateway.ENABLED
            && entitlementService.publicEndpointAccessGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId());
    }

    private Optional<TargetGroup> setupKnoxTargetGroup(Stack stack) {
        TargetGroup knoxTargetGroup = null;
        Set<String> knoxGatewayGroupNames = getKnoxGatewayGroups(stack);
        Set<InstanceGroup> knoxGatewayInstanceGroups = stack.getInstanceGroups().stream()
            .filter(ig -> knoxGatewayGroupNames.contains(ig.getGroupName()))
            .collect(Collectors.toSet());
        if (!knoxGatewayInstanceGroups.isEmpty()) {
            LOGGER.info("Knox gateway instance found; enabling Knox load balancer configuration.");
            knoxTargetGroup = new TargetGroup();
            knoxTargetGroup.setType(TargetGroupType.KNOX);
            knoxTargetGroup.setInstanceGroups(knoxGatewayInstanceGroups);
            TargetGroup finalKnoxTargetGroup = knoxTargetGroup;
            knoxGatewayInstanceGroups.forEach(ig -> ig.addTargetGroup(finalKnoxTargetGroup));
        }
        return Optional.ofNullable(knoxTargetGroup);
    }

    private LoadBalancer createLoadBalancerIfNotExists(Set<LoadBalancer> loadBalancers, LoadBalancerType type, Stack stack) {
        LoadBalancer loadBalancer;
        Optional<LoadBalancer> existingLoadBalancer = loadBalancers.stream()
            .filter(lb -> lb.getType() == type)
            .findFirst();
        if (existingLoadBalancer.isPresent()) {
            loadBalancer = existingLoadBalancer.get();
        } else {
            loadBalancer = new LoadBalancer();
            loadBalancer.setType(type);
            loadBalancer.setStack(stack);
            loadBalancers.add(loadBalancer);
        }
        return loadBalancer;
    }

    private void setupKnoxLoadBalancer(LoadBalancer loadBalancer, TargetGroup knoxTargetGroup) {
        loadBalancer.addTargetGroup(knoxTargetGroup);
        knoxTargetGroup.addLoadBalancer(loadBalancer);
    }
}
