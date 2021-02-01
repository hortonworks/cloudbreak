package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTransientDeploymentService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureTerminationHelperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTerminationHelperService.class);

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureComputeResourceService azureComputeResourceService;

    @Inject
    private AzureResourceConnector azureResourceConnector;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    private AzureTransientDeploymentService azureTransientDeploymentService;

    public List<CloudResource> handleTransientDeployment(AzureClient client, String resourceGroupName, String deploymentName) {

        List<CloudResource> deployedResources = azureTransientDeploymentService.handleTransientDeployment(client, resourceGroupName, deploymentName);
        if (CollectionUtils.isNotEmpty(deployedResources)) {
            List<CloudResource> osDiskResources = getOsDiskResources(client, resourceGroupName, deployedResources);
            deployedResources.addAll(osDiskResources);
        }
        return deployedResources;
    }

    private List<CloudResource> getOsDiskResources(AzureClient client, String resourceGroupName, List<CloudResource> deployedResources) {
        List<CloudResource> transientVms = deployedResources.stream()
                .filter(cloudResource -> ResourceType.AZURE_INSTANCE == cloudResource.getType())
                .collect(Collectors.toList());
        return azureCloudResourceService.getAttachedOsDiskResources(transientVms, resourceGroupName, client);
    }

    public List<CloudResourceStatus> downscale(AuthenticatedContext ac, CloudStack stack, List<CloudInstance> vms,
            List<CloudResource> allResources, List<CloudResource> resourcesToRemove) {
        List<CloudResource> networkResources = azureCloudResourceService.getNetworkResources(allResources);
        return terminateResources(ac, stack, vms, resourcesToRemove, networkResources, false);
    }

    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resourcesToRemove) {
        List<CloudInstance> vms = new ArrayList<>();
        stack.getGroups().forEach(group -> vms.addAll(group.getInstances()));
        List<CloudResource> networkResources = azureCloudResourceService.getNetworkResources(resourcesToRemove);
        return terminateResources(ac, stack, vms, resourcesToRemove, networkResources, true);
    }

    private List<CloudResourceStatus> terminateResources(AuthenticatedContext ac, CloudStack stack, List<CloudInstance> vms,
            List<CloudResource> resourcesToRemove, List<CloudResource> networkResources, boolean deleteWholeDeployment) {
        LOGGER.debug("Terminating the following resources: {}", resourcesToRemove);
        LOGGER.debug("Operation is: {}", deleteWholeDeployment ? "terminate" : "downscale");
        AzureClient client = ac.getParameter(AzureClient.class);

        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), stack);

        deleteInstancesInProgress(ac, vms, resourcesToRemove, resourceGroupName);
        azureUtils.deleteInstances(ac, vms);
        deleteCloudResourceList(ac, resourcesToRemove, ResourceType.AZURE_INSTANCE);

        List<String> networkInterfaceNames = getResourceNamesByResourceType(resourcesToRemove, ResourceType.AZURE_NETWORK_INTERFACE);
        azureUtils.waitForDetachNetworkInterfaces(ac, client, resourceGroupName, networkInterfaceNames);
        azureUtils.deleteNetworkInterfaces(client, resourceGroupName, networkInterfaceNames);
        deleteCloudResourceList(ac, resourcesToRemove, ResourceType.AZURE_NETWORK_INTERFACE);

        // load balancers must be deleted before public IP addresses
        deleteLoadBalancersIfNecessary(ac, resourcesToRemove, deleteWholeDeployment, client, resourceGroupName);

        List<String> publicAddressNames = getResourceNamesByResourceType(resourcesToRemove, ResourceType.AZURE_PUBLIC_IP);
        azureUtils.deletePublicIps(client, resourceGroupName, publicAddressNames);
        deleteCloudResourceList(ac, resourcesToRemove, ResourceType.AZURE_PUBLIC_IP);

        List<String> managedDiskNames = getResourceNamesByResourceType(resourcesToRemove, ResourceType.AZURE_DISK);
        azureUtils.deleteManagedDisks(client, resourceGroupName, managedDiskNames);
        deleteCloudResourceList(ac, resourcesToRemove, ResourceType.AZURE_DISK);

        deleteVolumeSets(ac, stack, resourcesToRemove, networkResources, resourceGroupName);

        if (deleteWholeDeployment) {
            // deleting availability sets
            List<String> availabiltySetNames = getResourceNamesByResourceType(resourcesToRemove, ResourceType.AZURE_AVAILABILITY_SET);
            azureUtils.deleteAvailabilitySets(client, resourceGroupName, availabiltySetNames);
            deleteCloudResourceList(ac, resourcesToRemove, ResourceType.AZURE_AVAILABILITY_SET);

            // deleting networks
            List<String> networkIds = getResourceIdsByResourceType(resourcesToRemove, ResourceType.AZURE_NETWORK);
            azureUtils.deleteNetworks(client, networkIds);
            deleteCloudResourceList(ac, resourcesToRemove, ResourceType.AZURE_NETWORK);
            deleteCloudResourceList(ac, resourcesToRemove, ResourceType.AZURE_SUBNET);

            // deleting security groups
            List<String> securityGroupIds = getResourceIdsByResourceType(resourcesToRemove, ResourceType.AZURE_SECURITY_GROUP);
            azureUtils.deleteSecurityGroups(client, securityGroupIds);
            deleteCloudResourceList(ac, resourcesToRemove, ResourceType.AZURE_SECURITY_GROUP);
        }

        LOGGER.debug("All the necessary resources have been deleted successfully");
        return azureResourceConnector.check(ac, resourcesToRemove);
    }

    private void deleteLoadBalancersIfNecessary(AuthenticatedContext ac,
                                                List<CloudResource> resourcesToRemove,
                                                boolean deleteWholeDeployment,
                                                AzureClient client,
                                                String resourceGroupName) {
        if (deleteWholeDeployment) {
            List<String> loadBalancerNames = getResourceNamesByResourceType(resourcesToRemove, ResourceType.AZURE_LOAD_BALANCER);
            azureUtils.deleteLoadBalancers(client, resourceGroupName, loadBalancerNames);
            deleteCloudResourceList(ac, resourcesToRemove, ResourceType.AZURE_LOAD_BALANCER);
        }
    }

    private void deleteInstancesInProgress(AuthenticatedContext ac, List<CloudInstance> vms, List<CloudResource> resourcesToRemove, String resourceGroupName) {
        List<CloudInstance> instancesInProgress = vms.stream()
                .filter(cloudInstance -> StringUtils.isEmpty(cloudInstance.getInstanceId()))
                .collect(Collectors.toList());
        if (instancesInProgress.size() > 0) {
            LOGGER.info("The following instances are not yet created: {}", instancesInProgress);
            List<String> instancesProgress = getResourceNamesByResourceType(resourcesToRemove, ResourceType.AZURE_INSTANCE);
            azureUtils.deleteInstancesByName(ac, resourceGroupName, instancesProgress);
            vms.removeAll(instancesInProgress);
        }
    }

    private void deleteVolumeSets(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resourcesToRemove,
            List<CloudResource> networkResources, String resourceGroupName) {
        try {
            List<CloudResource> volumeSetsToDelete =
                    resourcesToRemove.stream()
                            .filter(cloudResource -> cloudResource.getType().equals(ResourceType.AZURE_VOLUMESET))
                            .collect(Collectors.toList());
            azureComputeResourceService.deleteComputeResources(ac, stack, volumeSetsToDelete, networkResources);
        } catch (CloudConnectorException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CloudConnectorException(String.format("Failed to delete resources during downscale: %s", resourceGroupName), e);
        }
    }

    private void deleteCloudResourceList(AuthenticatedContext ac, List<CloudResource> resourcesToRemove, ResourceType type) {
        List<CloudResource> resourcesByType = getCloudResourcesByType(resourcesToRemove, type);
        azureCloudResourceService.deleteCloudResources(resourceNotifier, ac.getCloudContext(),
                resourcesByType);
        LOGGER.debug("The following cloud resources have been deleted from database: {}", resourcesByType.toString());
    }

    private List<CloudResource> getCloudResourcesByType(List<CloudResource> resourcesToRemove, ResourceType resourceType) {
        return resourcesToRemove.stream()
                .filter(cloudResource -> resourceType.equals(cloudResource.getType()))
                .collect(Collectors.toList());
    }

    private List<String> getResourceNamesByResourceType(List<CloudResource> resourcesToRemove, ResourceType resourceType) {
        return getCloudResourcesByType(resourcesToRemove, resourceType)
                .stream()
                .map(CloudResource::getName)
                .collect(Collectors.toList());
    }

    private List<String> getResourceIdsByResourceType(List<CloudResource> resourcesToRemove, ResourceType resourceType) {
        return getCloudResourcesByType(resourcesToRemove, resourceType)
                .stream()
                .map(CloudResource::getReference)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
