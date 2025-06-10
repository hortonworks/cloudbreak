package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.service.loadbalancer.NetworkLoadBalancerAttributeUtil.getLoadBalancerAttributeIfExists;
import static com.sequenceiq.cloudbreak.service.loadbalancer.NetworkLoadBalancerAttributeUtil.isSessionStickyForTargetGroup;
import static java.util.Map.entry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.NetworkV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.instance.AvailabilitySetNameService;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceGroupParameters;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.TargetGroupType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Service
public class LoadBalancerConfigService {

    public static final int DEFAULT_UPDATE_DOMAIN_COUNT = 20;

    public static final int DEFAULT_FAULT_DOMAIN_COUNT = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerConfigService.class);

    @Inject
    private SubnetSelector subnetSelector;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private AvailabilitySetNameService availabilitySetNameService;

    @Inject
    private LoadBalancerEnabler loadBalancerEnabler;

    @Inject
    private KnoxGroupDeterminer knoxGroupDeterminer;

    @Inject
    private OozieTargetGroupProvisioner oozieTargetGroupProvisioner;

    @Inject
    private LoadBalancerTypeDeterminer loadBalancerTypeDeterminer;

    public Optional<LoadBalancer> selectLoadBalancerForFrontend(Set<LoadBalancer> loadBalancers, LoadBalancerType preferredType) {
        Preconditions.checkNotNull(preferredType);
        Optional<LoadBalancer> loadBalancerOptional = getLoadBalancerForType(loadBalancers, preferredType);
        if (loadBalancerOptional.isPresent()) {
            LOGGER.debug("Found load balancer of type {}", preferredType);
        }
        if (loadBalancerOptional.isEmpty()) {
            loadBalancerOptional = getLoadBalancerForType(loadBalancers, LoadBalancerType.GATEWAY_PRIVATE);
            loadBalancerOptional.ifPresent(loadBalancer ->
                    LOGGER.debug("Could not find load balancer of preferred type {}. Using type {}", preferredType, loadBalancer.getType()));
        }
        if (loadBalancerOptional.isEmpty()) {
            loadBalancerOptional = loadBalancers.stream()
                    .filter(lb -> lb.getType() != null && !LoadBalancerType.OUTBOUND.equals(lb.getType()))
                    .findFirst();
            loadBalancerOptional.ifPresent(loadBalancer ->
                    LOGGER.debug("Could not find load balancer of preferred type {}. Using type {}", preferredType, loadBalancer.getType()));
        }
        if (loadBalancerOptional.isEmpty()) {
            LOGGER.debug("Unable to find load balancer");
        }
        return loadBalancerOptional;
    }

    private Optional<LoadBalancer> getLoadBalancerForType(Set<LoadBalancer> loadBalancers, LoadBalancerType preferredType) {
        return loadBalancers.stream()
                .filter(lb -> preferredType.equals(lb.getType()))
                .findFirst();
    }

    public boolean isLoadBalancerCreationConfigured(Stack stack, DetailedEnvironmentResponse environment) {
        return !setupLoadBalancers(stack, environment, true, false, null).isEmpty();
    }

    public Set<LoadBalancer> createLoadBalancers(Stack stack, DetailedEnvironmentResponse environment, StackV4Request source) {
        LoadBalancerSku sku = getLoadBalancerSku(source);
        boolean azureLoadBalancerDisabled = CloudPlatform.AZURE.toString().equalsIgnoreCase(stack.getCloudPlatform()) &&
                LoadBalancerSku.NONE.equals(sku);
        if (azureLoadBalancerDisabled) {
            Optional<TargetGroup> oozieTargetGroup = oozieTargetGroupProvisioner.setupOozieHATargetGroup(stack, true);
            if (oozieTargetGroup.isPresent()) {
                throw new CloudbreakServiceException("Unsupported setup: Load balancers are disabled, but Oozie HA is configured. " +
                        "Either enable Azure load balancers, or use a non-HA Oozie setup.");
            }
            LOGGER.debug("Azure load balancers have been explicitly disabled.");
            return Collections.emptySet();
        }

        boolean loadBalancerFlagEnabled = source != null && source.isEnableLoadBalancer();
        Set<LoadBalancer> loadBalancers = setupLoadBalancers(stack, environment, false, loadBalancerFlagEnabled, sku);

        if (stack.getCloudPlatform().equalsIgnoreCase(CloudPlatform.AZURE.toString())) {
            configureLoadBalancerAvailabilitySets(stack, loadBalancers);
            configureLoadBalancerSku(sku, loadBalancers);
        }
        return loadBalancers;
    }

    /**
     * Adds availability sets to instance groups associated with Knox and Oozie target groups.
     * This method updates the JSON parameters associated with certain instance groups.
     *
     * @param stack                 stack object
     * @param loadBalancers         the list of load balancers to look up target groups from.
     */
    private void configureLoadBalancerAvailabilitySets(Stack stack, Set<LoadBalancer> loadBalancers) {
        getKnoxInstanceGroups(stack, loadBalancers)
                .forEach(instanceGroup -> attachAvailabilitySetParameters(stack, instanceGroup));
        getOozieInstanceGroupsForAzure(stack, loadBalancers)
                .forEach(instanceGroup -> attachAvailabilitySetParameters(stack, instanceGroup));
    }

    private void attachAvailabilitySetParameters(Stack stack, InstanceGroup ig) {
        Map<String, Object> parameters = ig.getAttributes().getMap();
        parameters.put("availabilitySet", Map.ofEntries(
                entry(AzureInstanceGroupParameters.NAME, availabilitySetNameService.generateName(stack.getName(), ig.getGroupName())),
                entry(AzureInstanceGroupParameters.FAULT_DOMAIN_COUNT, DEFAULT_FAULT_DOMAIN_COUNT),
                entry(AzureInstanceGroupParameters.UPDATE_DOMAIN_COUNT, DEFAULT_UPDATE_DOMAIN_COUNT)));
        ig.setAttributes(new Json(parameters));
    }

    private Set<InstanceGroup> getKnoxInstanceGroups(Stack stack, Set<LoadBalancer> loadBalancers) {
        return getInstanceGroups(stack, loadBalancers, TargetGroupType.KNOX);
    }

    private Set<InstanceGroup> getOozieInstanceGroupsForAzure(Stack stack, Set<LoadBalancer> loadBalancers) {
        return getInstanceGroups(stack, loadBalancers, TargetGroupType.OOZIE);
    }

    private Set<InstanceGroup> getInstanceGroups(Stack stack, Set<LoadBalancer> loadBalancers, TargetGroupType type) {
        return loadBalancers.stream()
                .flatMap(loadBalancer -> loadBalancer.getTargetGroupSet().stream())
                .filter(targetGroup -> type.equals(targetGroup.getType()))
                .flatMap(targetGroup -> stack.getInstanceGroups().stream().filter(ig -> ig.getTargetGroups().contains(targetGroup)))
                .collect(Collectors.toSet());
    }

    /**
     * Sets the SKU of each load balancer to the SKU provided
     *
     * @param sku           The SKU extracted earlier for the stack.
     * @param loadBalancers The list of load balancers to update.
     */
    private void configureLoadBalancerSku(LoadBalancerSku sku, Set<LoadBalancer> loadBalancers) {
        loadBalancers.forEach(lb -> lb.setSku(sku));
    }

    private LoadBalancerSku getLoadBalancerSku(StackV4Request source) {
        return Optional.ofNullable(source)
                .map(StackV4Request::getAzure)
                .map(AzureStackV4Parameters::getLoadBalancerSku)
                .map(LoadBalancerSku::getValueOrDefault)
                .orElse(LoadBalancerSku.getDefault());
    }

    @VisibleForTesting
    Set<LoadBalancer> setupLoadBalancers(Stack stack, DetailedEnvironmentResponse environment, boolean dryRun, boolean loadBalancerFlagEnabled,
            LoadBalancerSku sku) {
        MDCBuilder.buildMdcContext(environment);
        if (dryRun) {
            LOGGER.info("Checking if load balancers are enabled and configurable for stack {}", stack.getName());
        } else {
            LOGGER.info("Setting up load balancers for stack {}", stack.getDisplayName());
        }
        Set<LoadBalancer> loadBalancers = new HashSet<>();

        if (loadBalancerEnabler.isLoadBalancerEnabled(stack.getType(), stack.getCloudPlatform(), environment, loadBalancerFlagEnabled)) {
            if (!loadBalancerFlagEnabled) {
                LOGGER.debug("Load balancers are enabled for data lake and data hub stacks.");
            } else {
                LOGGER.debug("Load balancer is explicitly defined for the stack.");
            }
            setupKnoxLoadbalancing(stack, environment, dryRun, loadBalancers, sku);
            setupOozieLoadbalancing(stack, dryRun, loadBalancers);

            // TODO CB-9368 - create target group for CM instances
        }

        LOGGER.debug("Adding {} load balancers for stack {}", loadBalancers.size(), stack.getName());
        return loadBalancers;
    }

    private void setupOozieLoadbalancing(Stack stack, boolean dryRun, Set<LoadBalancer> loadBalancers) {
        Optional<TargetGroup> oozieTargetGroup = oozieTargetGroupProvisioner.setupOozieHATargetGroup(stack, dryRun);
        if (oozieTargetGroup.isPresent()) {
            setupLoadBalancer(dryRun, stack, loadBalancers, oozieTargetGroup.get(), LoadBalancerType.PRIVATE);
        } else {
            LOGGER.debug("No Oozie HA instance group found.");
        }
    }

    private void setupKnoxLoadbalancing(Stack stack, DetailedEnvironmentResponse environment,
            boolean dryRun, Set<LoadBalancer> loadBalancers, LoadBalancerSku sku) {
        Optional<TargetGroup> knoxTargetGroup = setupKnoxTargetGroup(stack, dryRun);
        if (knoxTargetGroup.isPresent() && environment != null) {
            boolean createExternalLb = shouldCreateExternalKnoxLoadBalancer(stack.getNetwork(), environment.getNetwork(), stack.getCloudPlatform(),
                    environment.getAccountId());

            if (isNetworkUsingPrivateSubnet(stack.getNetwork(), environment.getNetwork())) {
                setupLoadBalancer(dryRun, stack, loadBalancers, knoxTargetGroup.get(), LoadBalancerType.PRIVATE);
                if (shouldCreateOutboundLoadBalancer(createExternalLb, stack, sku, environment.getNetwork())) {
                    LOGGER.debug("Found private only Azure load balancer configuration; creating outbound public load balancer for egress.");
                    setupLoadBalancer(dryRun, stack, loadBalancers, knoxTargetGroup.get(), LoadBalancerType.OUTBOUND);
                }
            } else {
                LOGGER.debug("Private subnet is not available. The internal load balancer will not be created.");
            }
            if (createExternalLb) {
                LoadBalancerType loadBalancerType = loadBalancerTypeDeterminer.getType(environment);
                LOGGER.debug("{} Endpoint Access Gateway is selected", loadBalancerType);
                setupLoadBalancer(dryRun, stack, loadBalancers, knoxTargetGroup.get(), loadBalancerType);
            } else {
                LOGGER.debug("External load balancer creation is disabled.");
            }
        } else {
            LOGGER.debug("No Knox instance groups found. If load balancer creation is enabled, Knox routing in the load balancer will be skipped.");
        }
    }

    private boolean shouldCreateOutboundLoadBalancer(boolean createExternalLb, Stack stack, LoadBalancerSku sku, EnvironmentNetworkResponse network) {
        return !createExternalLb
                && AZURE.equalsIgnoreCase(stack.getCloudPlatform())
                && LoadBalancerSku.STANDARD.equals(sku)
                && !Optional.of(network)
                            .map(EnvironmentNetworkResponse::getAzure)
                            .map(EnvironmentNetworkAzureParams::getNoOutboundLoadBalancer)
                            .orElse(network.isExistingNetwork());
    }

    private void setupLoadBalancer(boolean dryRun, Stack stack, Set<LoadBalancer> loadBalancers, TargetGroup targetGroup, LoadBalancerType type) {
        if (dryRun) {
            LOGGER.debug("{} load balancer can be configured for stack {}. Adding mock LB to configurable LB list.", type, stack.getName());
            LoadBalancer mockLb = new LoadBalancer();
            mockLb.setType(type);
            loadBalancers.add(mockLb);
        } else {
            LOGGER.debug("Found {} enabled instance groups in stack. Setting up {} load balancer.", targetGroup.getType(), type);
            setupLoadBalancerWithTargetGroup(
                    createLoadBalancerIfNotExists(loadBalancers, type, stack),
                    targetGroup);
        }
    }

    private boolean isNetworkUsingPrivateSubnet(Network network, EnvironmentNetworkResponse envNetwork) {
        return isSelectedSubnetAvailableAndRequestedType(network, envNetwork, true);
    }

    private boolean isNetworkUsingPublicSubnet(Network network, EnvironmentNetworkResponse envNetwork) {
        return isSelectedSubnetAvailableAndRequestedType(network, envNetwork, false);
    }

    private boolean isSelectedSubnetAvailableAndRequestedType(Network network, EnvironmentNetworkResponse envNetwork, boolean privateType) {
        if (network != null && envNetwork != null) {
            String subnetId = getSubnetId(network);
            if (StringUtils.isNotEmpty(subnetId)) {
                LOGGER.debug("Found selected stack subnet {}", subnetId);
                Optional<CloudSubnet> selectedSubnet = subnetSelector.findSubnetById(envNetwork.getSubnetMetas(), subnetId);

                // "noPublicIp" is an option for Azure and GCP that is used to set the network to private or public, it is not
                // set on AWS networks. So, we check for it first, then fall back to AWS.
                NetworkV4Base networkV4Base = getNetworkV4Base(network);
                if (networkV4Base.isNoPublicIp().isPresent()) {
                    // azure and gcp specific
                    Boolean noPublicIp = networkV4Base.isNoPublicIp().get();
                    LOGGER.debug("Subnet {} type {}", subnetId, noPublicIp ? "private" : "public");
                    return privateType == noPublicIp;
                } else if (selectedSubnet.isPresent()) {
                    // aws specific
                    LOGGER.debug("Subnet {} type {}", subnetId, selectedSubnet.get().isPrivateSubnet() ? "private" : "public");
                    return privateType == selectedSubnet.get().isPrivateSubnet();
                }
            }
        }
        LOGGER.debug("Subnet for load balancer creation was not found.");
        return false;
    }

    private String getSubnetId(Network network) {
        Json attributes = network.getAttributes();
        Map<String, Object> params = attributes == null ?
                Collections.emptyMap() : attributes.getMap();

        return params.get(NetworkConstants.SUBNET_ID) != null ? String.valueOf(params.get(NetworkConstants.SUBNET_ID)) : null;
    }

    private NetworkV4Base getNetworkV4Base(Network network) {
        Json attributes = network.getAttributes();
        Map<String, Object> params = attributes == null ?
                Collections.emptyMap() : attributes.getMap();
        NetworkV4Base networkV4Base = new NetworkV4Base();
        providerParameterCalculator.parse(params, networkV4Base);
        return networkV4Base;
    }

    private boolean shouldCreateExternalKnoxLoadBalancer(Network network, EnvironmentNetworkResponse envNetwork, String cloudPlatform, String accountId) {
        return CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform) ||
                loadBalancerEnabler.isEndpointGatewayEnabled(accountId, envNetwork) ||
                isNetworkUsingPublicSubnet(network, envNetwork);
    }

    private Optional<TargetGroup> setupKnoxTargetGroup(Stack stack, boolean dryRun) {
        TargetGroup knoxTargetGroup = null;
        Set<String> knoxGatewayGroupNames = knoxGroupDeterminer.getKnoxGatewayGroupNames(stack);
        Set<InstanceGroup> knoxGatewayInstanceGroups = stack.getInstanceGroups().stream()
                .filter(ig -> knoxGatewayGroupNames.contains(ig.getGroupName()))
                .collect(Collectors.toSet());

        if (AZURE.equalsIgnoreCase(stack.getCloudPlatform()) && knoxGatewayInstanceGroups.size() > 1) {
            throw new CloudbreakServiceException("For Azure load balancers, Knox must be defined in a single instance group.");
        } else if (!knoxGatewayInstanceGroups.isEmpty()) {
            LOGGER.info("Knox gateway instance found; enabling Knox load balancer configuration.");
            knoxTargetGroup = new TargetGroup();
            knoxTargetGroup.setType(TargetGroupType.KNOX);
            if (AWS.equalsIgnoreCase(stack.getCloudPlatform()) &&  stack.getNetwork() != null) {
                Optional<Map<String, Object>> loadBalancerAttributes = getLoadBalancerAttributeIfExists(stack.getNetwork().getAttributes());
                if (loadBalancerAttributes.isPresent()) {
                    knoxTargetGroup.setUseStickySession(isSessionStickyForTargetGroup(stack.getNetwork().getAttributes()));
                }
            }
            if (!dryRun) {
                LOGGER.debug("Adding target group to Knox gateway instances groups.");
                TargetGroup finalKnoxTargetGroup = knoxTargetGroup;
                knoxGatewayInstanceGroups.forEach(ig -> ig.addTargetGroup(finalKnoxTargetGroup));
            } else {
                LOGGER.debug("Dry run, skipping instance group/target group linkage.");
            }
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
            loadBalancer.setStackId(stack.getId());
            loadBalancers.add(loadBalancer);
        }
        return loadBalancer;
    }

    private void setupLoadBalancerWithTargetGroup(LoadBalancer loadBalancer, TargetGroup targetGroup) {
        loadBalancer.addTargetGroup(targetGroup);
        targetGroup.addLoadBalancer(loadBalancer);
    }
}
