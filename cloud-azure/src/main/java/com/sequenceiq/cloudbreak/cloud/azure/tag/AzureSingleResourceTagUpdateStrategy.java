package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_AVAILABILITY_SET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DISK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_LOAD_BALANCER;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK_INTERFACE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PUBLIC_IP;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_SECURITY_GROUP;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Handles tag update/deletion for every Azure resource type that is addressed by a single ID (either its Azure
 * resource reference or, for resource groups, its name) and exposes plain get/update tag calls on {@link AzureClient}.
 * The per-type differences (which {@link AzureClient} methods to call, and whether the resource is identified by
 * reference or by name) are captured in {@link #OPS_BY_TYPE} instead of one subclass per resource type.
 */
@Service
public class AzureSingleResourceTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSingleResourceTagUpdateStrategy.class);

    private static final Map<ResourceType, ResourceOps> OPS_BY_TYPE = Map.ofEntries(
            Map.entry(AZURE_INSTANCE, new ResourceOps("virtual machine", false,
                    AzureClient::getVirtualMachineTags, AzureClient::updateVirtualMachineTags)),
            Map.entry(AZURE_DISK, new ResourceOps("disk", false,
                    AzureClient::getDiskTags, AzureClient::updateDiskTags)),
            Map.entry(AZURE_LOAD_BALANCER, new ResourceOps("load balancer", false,
                    AzureClient::getLoadBalancerTags, AzureClient::updateLoadBalancerTags)),
            Map.entry(AZURE_NETWORK_INTERFACE, new ResourceOps("network interface", false,
                    AzureClient::getNetworkInterfaceTags, AzureClient::updateNetworkInterfaceTags)),
            Map.entry(AZURE_SECURITY_GROUP, new ResourceOps("network security group", false,
                    AzureClient::getNetworkSecurityGroupTags, AzureClient::updateNetworkSecurityGroupTags)),
            Map.entry(AZURE_PUBLIC_IP, new ResourceOps("public IP", false,
                    AzureClient::getPublicIpTags, AzureClient::updatePublicIpTags)),
            Map.entry(AZURE_AVAILABILITY_SET, new ResourceOps("availability set", false,
                    AzureClient::getAvailabilitySetTags, AzureClient::updateAvailabilitySetTags)),
            Map.entry(AZURE_RESOURCE_GROUP, new ResourceOps("resource group", true,
                    AzureClient::getResourceGroupTags, AzureClient::updateResourceGroupTags)));

    @Inject
    private AzureClientService azureClientService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return OPS_BY_TYPE.keySet();
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        ResourceOps ops = OPS_BY_TYPE.get(cloudResource.getType());
        String resourceId = resolveResourceId(cloudResource, ops);
        if (StringUtils.isBlank(resourceId)) {
            logSkippingTagUpdate(cloudResource, ops);
        } else {
            applyTagUpdate(azureClient(authenticatedContext), ops, resourceId, tags);
        }
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        ResourceOps ops = OPS_BY_TYPE.get(cloudResource.getType());
        String resourceId = resolveResourceId(cloudResource, ops);
        if (StringUtils.isBlank(resourceId)) {
            logSkippingTagDeletion(cloudResource, ops);
        } else {
            applyTagDeletion(azureClient(authenticatedContext), ops, resourceId, tagKeys);
        }
    }

    /**
     * Allows other Azure strategies (e.g. volume sets iterating over their individual disks) to reuse the same
     * read-merge-write logic for a resource type owned by this class, without going through {@link CloudResource}.
     */
    void applyTagUpdate(AzureClient azureClient, ResourceType resourceType, String resourceId, Map<String, String> tags) {
        applyTagUpdate(azureClient, OPS_BY_TYPE.get(resourceType), resourceId, tags);
    }

    /**
     * Allows other Azure strategies (e.g. volume sets iterating over their individual disks) to reuse the same
     * read-remove-write logic for a resource type owned by this class, without going through {@link CloudResource}.
     */
    void applyTagDeletion(AzureClient azureClient, ResourceType resourceType, String resourceId, Set<String> tagKeys) {
        applyTagDeletion(azureClient, OPS_BY_TYPE.get(resourceType), resourceId, tagKeys);
    }

    private void applyTagUpdate(AzureClient azureClient, ResourceOps ops, String resourceId, Map<String, String> tags) {
        Map<String, String> existingTags = ops.tagGetter().apply(azureClient, resourceId);
        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.debug("Tags for {} {} are already up to date, skipping update.", ops.label(), resourceId);
        } else {
            Map<String, String> mergedTags = mergeTags(existingTags, tags);
            logTagUpdate(LOGGER, resourceId, mergedTags);
            ops.tagUpdater().update(azureClient, resourceId, mergedTags);
        }
    }

    private void applyTagDeletion(AzureClient azureClient, ResourceOps ops, String resourceId, Set<String> tagKeys) {
        Map<String, String> existingTags = ops.tagGetter().apply(azureClient, resourceId);
        if (hasTagKeysToDelete(existingTags, tagKeys)) {
            Map<String, String> remainingTags = removeTagKeys(existingTags, tagKeys);
            logTagDeletion(LOGGER, resourceId, tagKeys, existingTags, remainingTags.keySet());
            ops.tagUpdater().update(azureClient, resourceId, remainingTags);
        } else {
            LOGGER.debug("No tags to delete for {} {}, skipping.", ops.label(), resourceId);
        }
    }

    private String resolveResourceId(CloudResource cloudResource, ResourceOps ops) {
        return ops.nameBased() ? cloudResource.getName() : cloudResource.getReference();
    }

    private void logSkippingTagUpdate(CloudResource cloudResource, ResourceOps ops) {
        if (ops.nameBased()) {
            LOGGER.warn("Skipping tag update for resource group: resource name is null.");
        } else {
            LOGGER.warn("Skipping tag update for {} ({}): resource reference is null.", cloudResource.getName(), cloudResource.getType());
        }
    }

    private void logSkippingTagDeletion(CloudResource cloudResource, ResourceOps ops) {
        if (ops.nameBased()) {
            LOGGER.warn("Skipping tag deletion for resource group: resource name is null.");
        } else {
            LOGGER.warn("Skipping tag deletion for {} ({}): resource reference is null.", cloudResource.getName(), cloudResource.getType());
        }
    }

    private AzureClient azureClient(AuthenticatedContext authenticatedContext) {
        return azureClientService.getClient(authenticatedContext.getCloudContext(), authenticatedContext.getCloudCredential());
    }

    private record ResourceOps(String label, boolean nameBased,
            BiFunction<AzureClient, String, Map<String, String>> tagGetter, AzureTagUpdater tagUpdater) {
    }

    @FunctionalInterface
    private interface AzureTagUpdater {
        void update(AzureClient azureClient, String resourceId, Map<String, String> tags);
    }
}
