package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.network.instancegroup.InstanceGroupNetworkService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class GcpSubnetIdFixPatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpSubnetIdFixPatchService.class);

    private static final String STACK_NETWORK_SUBNET_ID_ATTR_KEY = "subnetId";

    private static final String INSTANCE_GROUP_NETWORK_SUBNET_IDS_ATTR_KEY = "subnetIds";

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private NetworkService networkService;

    @Inject
    private InstanceGroupNetworkService instanceGroupNetworkService;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.GCP_SUBNET_ID_FIX;
    }

    @Override
    public boolean isAffected(Stack stack) {
        if (CloudPlatform.GCP.name().equalsIgnoreCase(stack.cloudPlatform())) {
            LOGGER.info("Checking GCP subnet ID mismatches for stack '{}'.", stack.getResourceCrn());
            Set<String> environmentSubnetIds = getEnvironmentSubnetIds(stack);

            Map<String, Object> stackNetworkAttributes = getStackNetworkAttributes(stack);
            String stackNetworkSubnetId = (String) stackNetworkAttributes.get(STACK_NETWORK_SUBNET_ID_ATTR_KEY);
            boolean stackNetworkMismatch = stackNetworkSubnetId != null && !environmentSubnetIds.contains(stackNetworkSubnetId);

            Map<String, Map<String, Object>> instanceGroupNetworkAttributesMap = getInstanceGroupNetworkAttributesMap(stack);
            boolean instanceGroupNetworkMismatch = instanceGroupNetworkAttributesMap.values().stream()
                    .anyMatch(attributes -> {
                        List<String> subnetIds = (List<String>) attributes.get(INSTANCE_GROUP_NETWORK_SUBNET_IDS_ATTR_KEY);
                        return subnetIds != null && subnetIds.stream().anyMatch(subnetId -> !environmentSubnetIds.contains(subnetId));
                    });

            List<Resource> subnetResources = getSubnetResources(stack);
            boolean subnetResourcesMismatch = subnetResources.stream()
                    .anyMatch(resource -> {
                        String resourceName = resource.getResourceName();
                        return resourceName != null && !environmentSubnetIds.contains(resourceName);
                    });

            LOGGER.info("Stack '{}' has the following subnet ID mismatches - stack network: {}, instance group networks: {}, subnet resources: {}",
                    stack.getResourceCrn(), stackNetworkMismatch, instanceGroupNetworkMismatch, subnetResourcesMismatch);
            return stackNetworkMismatch || instanceGroupNetworkMismatch || subnetResourcesMismatch;
        } else {
            return false;
        }
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        Set<String> environmentSubnetIds = getEnvironmentSubnetIds(stack);
        Map<String, Object> stackNetworkAttributes = getStackNetworkAttributes(stack);
        Map<String, Map<String, Object>> instanceGroupNetworkAttributesMap = getInstanceGroupNetworkAttributesMap(stack);
        List<Resource> subnetResources = getSubnetResources(stack);
        Map<String, String> providerSideIdToSubnetIdMap = getProviderSideIdToSubnetIdMap(stack, stackNetworkAttributes, environmentSubnetIds);
        LOGGER.info("Provider side ID to subnet ID map for stack '{}': '{}'", stack.getResourceCrn(), providerSideIdToSubnetIdMap);
        try {
            transactionService.required(() -> {
                updateNetworkTable(stack, stackNetworkAttributes, providerSideIdToSubnetIdMap);
                updateInstanceGroupNetworkTable(stack, instanceGroupNetworkAttributesMap, providerSideIdToSubnetIdMap);
                updateResourceTable(stack, subnetResources, providerSideIdToSubnetIdMap);
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Failed to fix GCP subnet IDs for stack '{}' because of an unexpected error.", stack.getResourceCrn(), e);
            return false;
        }
        LOGGER.info("Successfully fixed GCP subnet IDs for stack '{}'.", stack.getResourceCrn());
        return true;
    }

    private Set<String> getEnvironmentSubnetIds(Stack stack) {
        DetailedEnvironmentResponse environment = environmentService.getByCrn(stack.getEnvironmentCrn());
        Set<String> environmentSubnetIds = environment.getNetwork().getSubnetIds();
        LOGGER.info("Environment '{}' subnet IDs: '{}'", environment.getCrn(), environmentSubnetIds);
        return environmentSubnetIds;
    }

    private Map<String, Object> getStackNetworkAttributes(Stack stack) {
        Map<String, Object> stackNetworkAttributes = Optional.ofNullable(stack)
                .map(Stack::getNetwork)
                .map(Network::getAttributes)
                .map(Json::getMap)
                .orElseThrow();
        LOGGER.info("Stack '{}' network attributes: '{}'", stack.getResourceCrn(), stackNetworkAttributes);
        return stackNetworkAttributes;
    }

    private Map<String, Map<String, Object>> getInstanceGroupNetworkAttributesMap(Stack stack) {
        Map<String, Map<String, Object>> instanceGroupNameNetworkAttributesMap = new HashMap<>();
        for (InstanceGroup ig : stack.getInstanceGroups()) {
            Map<String, Object> igNetworkAttributes = Optional.ofNullable(ig)
                    .map(InstanceGroup::getInstanceGroupNetwork)
                    .map(InstanceGroupNetwork::getAttributes)
                    .map(Json::getMap)
                    .orElseThrow();
            instanceGroupNameNetworkAttributesMap.put(ig.getGroupName(), igNetworkAttributes);
        }
        LOGGER.info("Stack '{}' instance groups and their network attributes: '{}'", stack.getResourceCrn(), instanceGroupNameNetworkAttributesMap);
        return instanceGroupNameNetworkAttributesMap;
    }

    private List<Resource> getSubnetResources(Stack stack) {
        List<Resource> subnetResources = resourceService.findByStackIdAndType(stack.getId(), ResourceType.GCP_SUBNET);
        LOGGER.info("GCP_SUBNET cloud resources for stack '{}': '{}'", stack.getResourceCrn(), subnetResources);
        return subnetResources;
    }

    private Map<String, String> getProviderSideIdToSubnetIdMap(Stack stack, Map<String, Object> stackNetworkAttributes, Set<String> environmentSubnetIds) {
        Map<String, String> filters = new HashMap<>();
        putIfPresent(filters, GcpStackUtil.NETWORK_ID, (String) stackNetworkAttributes.get(GcpStackUtil.NETWORK_ID));
        putIfPresent(filters, GcpStackUtil.SHARED_PROJECT_ID, (String) stackNetworkAttributes.get(GcpStackUtil.SHARED_PROJECT_ID));
        putIfPresent(filters, NetworkConstants.SUBNET_IDS, environmentSubnetIds.stream().collect(Collectors.joining(",")));
        ExtendedCloudCredential extendedCloudCredential = credentialClientService.getExtendedCloudCredential(stack.getEnvironmentCrn());
        CloudNetworks cloudNetworks = cloudParameterService.getCloudNetworks(extendedCloudCredential, stack.getRegion(), stack.getPlatformVariant(), filters);
        Map<String, String> providerSideIdToSubnetIdMap = cloudNetworks.getCloudNetworkResponses().values().stream()
                .flatMap(Set::stream)
                .map(CloudNetwork::getSubnetsMeta)
                .flatMap(Set::stream)
                .collect(Collectors.toMap(cs -> cs.getParameter(NetworkConstants.PROVIDER_SIDE_ID, String.class), CloudSubnet::getId));
        return providerSideIdToSubnetIdMap;
    }

    private void updateNetworkTable(Stack stack, Map<String, Object> stackNetworkAttributes, Map<String, String> providerSideIdToSubnetIdMap) {
        String stackNetworkSubnetId = (String) stackNetworkAttributes.get(STACK_NETWORK_SUBNET_ID_ATTR_KEY);
        if (stackNetworkSubnetId != null) {
            String resolvedStackNetworkSubnetId = providerSideIdToSubnetIdMap.get(stackNetworkSubnetId);
            if (resolvedStackNetworkSubnetId != null) {
                stackNetworkAttributes.put(STACK_NETWORK_SUBNET_ID_ATTR_KEY, resolvedStackNetworkSubnetId);
                stack.getNetwork().setAttributes(new Json(stackNetworkAttributes));
                LOGGER.info("Updated stack '{}' network subnet ID from '{}' to '{}'", stack.getResourceCrn(), stackNetworkSubnetId,
                        resolvedStackNetworkSubnetId);
                networkService.savePure(stack.getNetwork());
            } else {
                LOGGER.info("Could not resolve provider side subnet ID '{}' for stack '{}' network. The subnet ID might have already been fixed.",
                        stackNetworkSubnetId, stack.getResourceCrn());
            }
        } else {
            LOGGER.info("Stack '{}' network does not have a subnet ID in its attributes, therefore skipping the update.", stack.getResourceCrn());
        }
    }

    private void updateInstanceGroupNetworkTable(Stack stack, Map<String, Map<String, Object>> instanceGroupNameNetworkAttributesMap,
            Map<String, String> providerSideIdToSubnetIdMap) {
        List<InstanceGroup> updatedInstanceGroups = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> instanceGroup : instanceGroupNameNetworkAttributesMap.entrySet()) {
            String groupName = instanceGroup.getKey();
            Map<String, Object> instanceGroupNetworkAttributes = instanceGroup.getValue();
            List<String> instanceGroupNetworkSubnetIds = (List<String>) instanceGroupNetworkAttributes.get(INSTANCE_GROUP_NETWORK_SUBNET_IDS_ATTR_KEY);
            if (instanceGroupNetworkSubnetIds != null) {
                List<String> resolvedInstanceGroupNetworkSubnetIds = instanceGroupNetworkSubnetIds.stream()
                        .map(providerSideIdToSubnetIdMap::get)
                        .filter(subnetId -> subnetId != null)
                        .toList();
                if (!resolvedInstanceGroupNetworkSubnetIds.isEmpty()) {
                    instanceGroupNetworkAttributes.put(INSTANCE_GROUP_NETWORK_SUBNET_IDS_ATTR_KEY, resolvedInstanceGroupNetworkSubnetIds);
                    InstanceGroup igToUpdate = stack.getInstanceGroups().stream()
                            .filter(ig -> groupName.equals(ig.getGroupName()))
                            .findFirst()
                            .orElseThrow();
                    igToUpdate.getInstanceGroupNetwork().setAttributes(new Json(instanceGroupNetworkAttributes));
                    LOGGER.info("Updated stack '{}' instance group '{}' subnet IDs from '{}' to '{}'", stack.getResourceCrn(), groupName,
                            instanceGroupNetworkSubnetIds, resolvedInstanceGroupNetworkSubnetIds);
                    updatedInstanceGroups.add(igToUpdate);
                } else {
                    LOGGER.info("Could not resolve any provider side subnet IDs '{}' for stack '{}' instance group '{}'. " +
                            "The subnet IDs might have already been fixed.", instanceGroupNetworkSubnetIds, stack.getResourceCrn(), groupName);
                }
            } else {
                LOGGER.info("Stack '{}' instance group '{}' does not have subnet IDs in its network attributes, therefore skipping the update.",
                        stack.getResourceCrn(), groupName);
            }
        }
        instanceGroupNetworkService.saveAll(updatedInstanceGroups.stream().map(InstanceGroup::getInstanceGroupNetwork).toList());
    }

    private void updateResourceTable(Stack stack, List<Resource> subnetResources, Map<String, String> providerSideIdToSubnetIdMap) {
        List<Resource> updatedResources = new ArrayList<>();
        for (Resource subnetResource : subnetResources) {
            String resourceName = subnetResource.getResourceName();
            if (resourceName != null) {
                String resolvedSubnetId = providerSideIdToSubnetIdMap.get(resourceName);
                if (resolvedSubnetId != null) {
                    subnetResource.setResourceName(resolvedSubnetId);
                    LOGGER.info("Updated GCP_SUBNET resource '{}' resource name from '{}' to '{}' for stack '{}'", subnetResource.getId(), resourceName,
                            resolvedSubnetId, stack.getResourceCrn());
                    updatedResources.add(subnetResource);
                } else {
                    LOGGER.info("Could not resolve provider side subnet ID '{}' for GCP_SUBNET resource '{}' of stack '{}'. " +
                            "The subnet ID might have already been fixed.", resourceName, subnetResource.getId(), stack.getResourceCrn());
                }
            } else {
                LOGGER.info("GCP_SUBNET resource '{}' of stack '{}' does not have a resource name, therefore skipping the update.",
                        subnetResource.getId(), stack.getResourceCrn());
            }
        }
        resourceService.saveAll(updatedResources);
    }
}
