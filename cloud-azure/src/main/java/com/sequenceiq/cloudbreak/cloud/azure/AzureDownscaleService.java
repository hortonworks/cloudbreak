package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;

@Service
public class AzureDownscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDownscaleService.class);

    private static final String NETWORK_INTERFACES_NAMES = "NETWORK_INTERFACES_NAMES";

    private static final String PUBLIC_ADDRESS_NAME = "PUBLIC_ADDRESS_NAME";

    private static final String AVAILABILITY_SET_NAME = "AVAILABILITY_SET_NAME";

    private static final String MANAGED_DISK_IDS = "MANAGED_DISK_IDS";

    private static final String STORAGE_PROFILE_DISK_NAMES = "STORAGE_PROFILE_DISK_NAMES";

    private static final String ATTACHED_DISK_STORAGE_NAME = "ATTACHED_DISK_STORAGE_NAME";

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureVirtualMachineService azureVirtualMachineService;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureComputeResourceService azureComputeResourceService;

    @Inject
    private AzureResourceConnector azureResourceConnector;

    public List<CloudResourceStatus> downscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            Map<String, Map<String, Object>> resourcesToRemove) {
        return terminateResources(ac, stack, resources, vms, resourcesToRemove, false);
    }

    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            Map<String, Map<String, Object>> resourcesToRemove) {
        return terminateResources(ac, stack, resources, vms, resourcesToRemove, true);
    }

    private List<CloudResourceStatus> terminateResources(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            Map<String, Map<String, Object>> resourcesToRemove, boolean deleteAvailabilitySets) {
        AzureClient client = ac.getParameter(AzureClient.class);
        String diskContainer = azureStorage.getDiskContainerName(ac.getCloudContext());

        List<CloudResource> networkResources = cloudResourceHelper.getNetworkResources(resources);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), stack);

        Map<String, VirtualMachine> vmsFromAzure = azureVirtualMachineService.getVmsFromAzureAndFillStatuses(ac, vms, new ArrayList<>());
        List<CloudInstance> cloudInstancesSyncedWithAzure = vms.stream()
                .filter(cloudInstance -> vmsFromAzure.containsKey(cloudInstance.getInstanceId()))
                .collect(Collectors.toList());
        azureUtils.deleteInstances(ac, cloudInstancesSyncedWithAzure);

        List<String> networkInterfaceNames = getResourcesByResourceType(resourcesToRemove, NETWORK_INTERFACES_NAMES);
        azureUtils.waitForDetachNetworkInterfaces(ac, client, resourceGroupName, networkInterfaceNames);
        azureUtils.deleteNetworkInterfaces(client, resourceGroupName, networkInterfaceNames);

        List<String> publicAddressNames = getResourcesByResourceType(resourcesToRemove, PUBLIC_ADDRESS_NAME);
        azureUtils.deletePublicIps(client, resourceGroupName, publicAddressNames);

        List<String> managedDiskIds = getResourcesByResourceType(resourcesToRemove, MANAGED_DISK_IDS);
        azureUtils.deleteManagedDisks(client, managedDiskIds);

        for (CloudInstance instance : vms) {
            String instanceId = instance.getInstanceId();
            Map<String, Object> instanceResources = resourcesToRemove.get(instanceId);
            try {
                if (instanceResources != null) {
                    // TODO: blocking foreach!
                    //       it can be slow only on clusters with unmanaged disks!
                    deleteDisk(CollectionUtils.emptyIfNull((Collection<String>) instanceResources.get(STORAGE_PROFILE_DISK_NAMES)), client, resourceGroupName,
                            (String) instanceResources.get(ATTACHED_DISK_STORAGE_NAME), diskContainer);
                    if (azureStorage.getArmAttachedStorageOption(stack.getParameters()) == ArmAttachedStorageOption.PER_VM) {
                        azureStorage.deleteStorage(client, (String) instanceResources.get(ATTACHED_DISK_STORAGE_NAME), resourceGroupName);
                    }
                    List<CloudResource> resourcesToDownscale = resources.stream()
                            .filter(resource -> vms.stream()
                                    .map(CloudInstance::getInstanceId)
                                    .collect(Collectors.toList())
                                    .contains(resource.getInstanceId()))
                            .collect(Collectors.toList());
                    azureComputeResourceService.deleteComputeResources(ac, stack, resourcesToDownscale, networkResources);
                }
            } catch (CloudConnectorException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new CloudConnectorException(String.format("Failed to cleanup resources after downscale: %s", resourceGroupName), e);
            }
        }

        if (deleteAvailabilitySets) {
            List<String> availabiltySetNames = getResourcesByResourceType(resourcesToRemove, AVAILABILITY_SET_NAME);
            azureUtils.deleteAvailabilitySets(client, resourceGroupName, availabiltySetNames);
        }

        return azureResourceConnector.check(ac, resources);
    }

    private List<String> getResourcesByResourceType(Map<String, Map<String, Object>> resourcesToRemove, String resourceType) {
        return resourcesToRemove.entrySet().stream()
                .filter(instanceResources -> instanceResources.getValue().get(resourceType) != null)
                .flatMap(instanceResources -> ((Collection<String>) instanceResources.getValue().get(resourceType)).stream())
                .collect(Collectors.toList());
    }

    private void deleteDisk(Collection<String> storageProfileDiskNames, AzureClient azureClient, String resourceGroup, String storageName, String container) {
        for (String storageProfileDiskName : storageProfileDiskNames) {
            try {
                azureClient.deleteBlobInStorageContainer(resourceGroup, storageName, container, storageProfileDiskName);
            } catch (CloudException e) {
                if (e.response().code() != AzureConstants.NOT_FOUND) {
                    throw new CloudConnectorException(String.format("Could not delete blob: %s", storageProfileDiskName), e);
                } else {
                    LOGGER.debug("Disk not found: resourceGroup={}, storageName={}, container={}, storageProfileDiskName: {}",
                            resourceGroup, storageName, container, storageProfileDiskName);
                }
            }
        }
    }
}
