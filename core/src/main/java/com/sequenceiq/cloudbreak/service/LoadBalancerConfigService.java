package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static java.util.Map.entry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.NetworkV4Base;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceGroupParameters;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.TargetGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Service
public class LoadBalancerConfigService {

    public static final int DEFAULT_UPDATE_DOMAIN_COUNT = 20;

    public static final int DEFAULT_FAULT_DOMAIN_COUNT = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerConfigService.class);

    private static final String ENDPOINT_SUFFIX = "gateway";

    @Value("${cb.https.port:443}")
    private String httpsPort;

    @Value("${cb.knox.port:8443}")
    private String knoxServicePort;

    @Value("${cb.loadBalancer.supportedPlatforms:}")
    private String supportedPlatforms;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SubnetSelector subnetSelector;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

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

    public String generateLoadBalancerEndpoint(Stack stack) {
        StringBuilder name = new StringBuilder()
            .append(stack.getName())
            .append('-')
            .append(ENDPOINT_SUFFIX);
        return name.toString();
    }

    public Set<TargetGroupPortPair> getTargetGroupPortPairs(TargetGroup targetGroup) {
        switch (targetGroup.getType()) {
            case KNOX:
                return Set.of(new TargetGroupPortPair(Integer.parseInt(httpsPort), Integer.parseInt(knoxServicePort)));
            default:
                return null;
        }
    }

    /*
     * favors public over private
     */
    public String getLoadBalancerUserFacingFQDN(Long stackId) {
        Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stackId).stream()
                .filter(lb -> StringUtils.isNotBlank(lb.getDns()) ||
                        StringUtils.isNotBlank(lb.getIp()) || StringUtils.isNotBlank(lb.getFqdn()))
                .collect(Collectors.toSet());

        return findPublicLbName(loadBalancers)
                .orElseGet(() -> findPrivateLbNameOrNull(loadBalancers));
    }

    private Optional<String> findPublicLbName(Set<LoadBalancer> loadBalancers) {
        return loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
                .findAny().map(this::getBestAddressable);
    }

    private String findPrivateLbNameOrNull(Set<LoadBalancer> loadBalancers) {
        return loadBalancers.stream().findAny()
                .map(this::getBestAddressable).orElse(null);
    }

    private String getBestAddressable(LoadBalancer lb) {
        if (StringUtils.isNotBlank(lb.getFqdn())) {
            return lb.getFqdn();
        }
        if (StringUtils.isNotBlank(lb.getDns())) {
            return lb.getDns();
        }
        return lb.getIp();
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

    public boolean isLoadBalancerCreationConfigured(Stack stack, DetailedEnvironmentResponse environment) {
        return !setupLoadBalancers(stack, environment, true, false).isEmpty();
    }

    public Set<LoadBalancer> createLoadBalancers(Stack stack, DetailedEnvironmentResponse environment, boolean loadBalancerFlagEnabled) {
        Set<LoadBalancer> loadBalancers = setupLoadBalancers(stack, environment, false, loadBalancerFlagEnabled);

        if (stack.getCloudPlatform().equalsIgnoreCase(CloudPlatform.AZURE.toString())) {
            configureLoadBalancerAvailabilitySets(stack.getName(), loadBalancers);
        }
        return loadBalancers;
    }

    /**
     * Adds availability sets to instance groups associated with Knox target groups.
     * This method updates the JSON parameters associated with certain instance groups.
     *
     * It's only necessary to add availability sets to the Azure deployment while we
     * use the {@code basic} Azure Load Balancer SKU.
     *
     * @param availabilitySetPrefix A string prefix. Should be a stack name normally.
     * @param loadBalancers the list of load balancers to look up target groups from.
     */
    public void configureLoadBalancerAvailabilitySets(String availabilitySetPrefix, Set<LoadBalancer> loadBalancers) {
        getKnoxInstanceGroups(loadBalancers)
                .forEach(instanceGroup -> attachAvailabilitySetParameters(availabilitySetPrefix, instanceGroup));
    }

    private void attachAvailabilitySetParameters(String availabilitySetPrefix, InstanceGroup ig) {
        Map<String, Object> parameters = ig.getAttributes().getMap();
        parameters.put("availabilitySet", Map.ofEntries(
                entry(AzureInstanceGroupParameters.NAME, String.format("%s-%s-as", availabilitySetPrefix, ig.getGroupName())),
                entry(AzureInstanceGroupParameters.FAULT_DOMAIN_COUNT, DEFAULT_FAULT_DOMAIN_COUNT),
                entry(AzureInstanceGroupParameters.UPDATE_DOMAIN_COUNT, DEFAULT_UPDATE_DOMAIN_COUNT)));
        ig.setAttributes(new Json(parameters));
    }

    private Set<InstanceGroup> getKnoxInstanceGroups(Set<LoadBalancer> loadBalancers) {
        return loadBalancers.stream()
                .flatMap(loadBalancer -> loadBalancer.getTargetGroupSet().stream())
                .filter(targetGroup -> TargetGroupType.KNOX.equals(targetGroup.getType()))
                .flatMap(targetGroup -> targetGroup.getInstanceGroups().stream())
                .collect(Collectors.toSet());
    }

    public Set<LoadBalancer> setupLoadBalancers(Stack stack, DetailedEnvironmentResponse environment, boolean dryRun, boolean loadBalancerFlagEnabled) {
        if (dryRun) {
            LOGGER.info("Checking if load balancers are enabled and configurable for stack {}", stack.getName());
        } else {
            LOGGER.info("Setting up load balancers for stack {}", stack.getDisplayName());
        }
        Set<LoadBalancer> loadBalancers = new HashSet<>();

        if (isLoadBalancerEnabled(stack.getType(), stack.getCloudPlatform(), environment, loadBalancerFlagEnabled)) {
            if (!loadBalancerFlagEnabled) {
                LOGGER.debug("Load balancers are enabled for data lake and data hub stacks.");
            } else {
                LOGGER.debug("Load balancer is explicitly defined for the stack.");
            }
            Optional<TargetGroup> knoxTargetGroup = setupKnoxTargetGroup(stack, dryRun);
            if (knoxTargetGroup.isPresent()) {
                if (isNetworkUsingPrivateSubnet(stack.getNetwork(), environment.getNetwork())) {
                    setupLoadBalancer(dryRun, stack, loadBalancers, knoxTargetGroup.get(), LoadBalancerType.PRIVATE);
                } else {
                    LOGGER.debug("Private subnet is not available. The internal load balancer will not be created.");
                }
                if (shouldCreateExternalKnoxLoadBalancer(stack.getNetwork(), environment.getNetwork(), stack.getCloudPlatform())) {
                    setupLoadBalancer(dryRun, stack, loadBalancers, knoxTargetGroup.get(), LoadBalancerType.PUBLIC);
                } else {
                    LOGGER.debug("External load balancer creation is disabled.");
                }
            } else {
                LOGGER.debug("No Knox instance groups found. If load balancer creation is enabled, Knox routing in the load balancer will be skipped.");
            }

            // TODO CB-9368 - create target group for CM instances
        }

        LOGGER.debug("Adding {} load balancers for stack {}", loadBalancers.size(), stack.getName());
        return loadBalancers;
    }

    private void setupLoadBalancer(boolean dryRun, Stack stack, Set<LoadBalancer> loadBalancers, TargetGroup knoxTargetGroup,
            LoadBalancerType type) {
        if (dryRun) {
            LOGGER.debug("{} load balancer can be configured for stack {}. Adding mock LB to configurable LB list.", type, stack.getName());
            LoadBalancer mockLb = new LoadBalancer();
            mockLb.setType(type);
            loadBalancers.add(mockLb);
        } else {
            LOGGER.debug("Found Knox enabled instance groups in stack. Setting up {} Knox load balancer.", type);
            setupKnoxLoadBalancer(
                createLoadBalancerIfNotExists(loadBalancers, type, stack),
                knoxTargetGroup);
        }
    }

    private boolean isNetworkUsingPrivateSubnet(Network network, EnvironmentNetworkResponse envNetwork) {
        return isSelectedSubnetAvailableAndRequestedType(network, envNetwork, true);
    }

    private boolean isNetworkUsingPublicSubnet(Network network, EnvironmentNetworkResponse envNetwork) {
        return isSelectedSubnetAvailableAndRequestedType(network, envNetwork, false);
    }

    private boolean isSelectedSubnetAvailableAndRequestedType(Network network, EnvironmentNetworkResponse envNetwork, boolean privateType) {
        if (network != null) {
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

    private boolean isLoadBalancerEnabled(StackType type, String cloudPlatform, DetailedEnvironmentResponse environment, boolean flagEnabled) {
        return getSupportedPlatforms().contains(cloudPlatform) &&
            (flagEnabled || isLoadBalancerEnabledForDatalake(type, environment) || isLoadBalancerEnabledForDatahub(type, environment));
    }

    private boolean isLoadBalancerEnabledForDatalake(StackType type, DetailedEnvironmentResponse environment) {
        return StackType.DATALAKE.equals(type) && environment != null &&
                (entitlementService.datalakeLoadBalancerEnabled(ThreadBasedUserCrnProvider.getAccountId()) ||
                !isLoadBalancerEntitlementRequiredForCloudProvider(environment.getCloudPlatform()) ||
                isEndpointGatewayEnabled(environment.getNetwork()));
    }

    private boolean isLoadBalancerEntitlementRequiredForCloudProvider(String cloudPlatform) {
        return !(AWS.equalsIgnoreCase(cloudPlatform));
    }

    private boolean isLoadBalancerEnabledForDatahub(StackType type, DetailedEnvironmentResponse environment) {
        return StackType.WORKLOAD.equals(type) && environment != null && isEndpointGatewayEnabled(environment.getNetwork());
    }

    private boolean shouldCreateExternalKnoxLoadBalancer(Network network, EnvironmentNetworkResponse envNetwork, String cloudPlatform) {
        return CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform) ||
                isEndpointGatewayEnabled(envNetwork) ||
                isNetworkUsingPublicSubnet(network, envNetwork);
    }

    private boolean isEndpointGatewayEnabled(EnvironmentNetworkResponse network) {
        boolean result =  network != null && network.getPublicEndpointAccessGateway() == PublicEndpointAccessGateway.ENABLED;
        if (result) {
            LOGGER.debug("Public endpoint access gateway is enabled. A public load balancer will be created.");
        } else {
            LOGGER.debug("Public endpoint access gateway is disabled.");
        }
        return result;
    }

    private Optional<TargetGroup> setupKnoxTargetGroup(Stack stack, boolean dryRun) {
        TargetGroup knoxTargetGroup = null;
        Set<String> knoxGatewayGroupNames = getKnoxGatewayGroups(stack);
        Set<InstanceGroup> knoxGatewayInstanceGroups = stack.getInstanceGroups().stream()
            .filter(ig -> knoxGatewayGroupNames.contains(ig.getGroupName()))
            .collect(Collectors.toSet());

        if (AZURE.equalsIgnoreCase(stack.getCloudPlatform()) && knoxGatewayInstanceGroups.size() > 1) {
            throw new CloudbreakServiceException("For Azure load balancers, Knox must be defined in a single instance group.");
        } else if (!knoxGatewayInstanceGroups.isEmpty()) {
            LOGGER.info("Knox gateway instance found; enabling Knox load balancer configuration.");
            knoxTargetGroup = new TargetGroup();
            knoxTargetGroup.setType(TargetGroupType.KNOX);
            knoxTargetGroup.setInstanceGroups(knoxGatewayInstanceGroups);
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
            loadBalancer.setStack(stack);
            loadBalancers.add(loadBalancer);
        }
        return loadBalancer;
    }

    private void setupKnoxLoadBalancer(LoadBalancer loadBalancer, TargetGroup knoxTargetGroup) {
        loadBalancer.addTargetGroup(knoxTargetGroup);
        knoxTargetGroup.addLoadBalancer(loadBalancer);
    }

    private List<String> getSupportedPlatforms() {
        return supportedPlatforms == null ? List.of() : Arrays.asList(supportedPlatforms.split(","));
    }
}
