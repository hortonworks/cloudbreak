package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.DataDisk;
import com.microsoft.azure.management.compute.OSDisk;
import com.microsoft.azure.management.compute.StorageProfile;
import com.microsoft.azure.management.compute.VirtualHardDisk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import com.microsoft.azure.management.network.NicIpConfiguration;
import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStorageView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class AzureResourceConnector implements ResourceConnector<Map<String, Map<String, Object>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceConnector.class);

    private static final Double PUBLIC_ADDRESS_BATCH_RATIO = 100D / 30;

    private static final String NETWORK_INTERFACES_NAMES = "NETWORK_INTERFACES_NAMES";

    private static final String STORAGE_PROFILE_DISK_NAMES = "STORAGE_PROFILE_DISK_NAMES";

    private static final String ATTACHED_DISK_STORAGE_NAME = "ATTACHED_DISK_STORAGE_NAME";

    private static final String PUBLIC_ADDRESS_NAME = "PUBLIC_ADDRESS_NAME";

    @Value("${cb.azure.host.name.prefix.length}")
    private int stackNamePrefixLength;

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureTemplateBuilder azureTemplateBuilder;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureStorage azureStorage;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentType adjustmentType, Long threshold) {
        AzureCredentialView azureCredentialView = new AzureCredentialView(ac.getCloudCredential());
        String stackName = azureUtils.getStackName(ac.getCloudContext());
        String resourceGroupName = azureUtils.getResourceGroupName(ac.getCloudContext());
        AzureStackView azureStackView = getAzureStack(azureCredentialView, ac.getCloudContext(), stack);
        azureUtils.validateStorageType(stack);
        String template = azureTemplateBuilder.build(stackName, azureCredentialView, azureStackView, ac.getCloudContext(), stack);
        String parameters = azureTemplateBuilder.buildParameters(ac.getCloudCredential(), stack.getNetwork(), stack.getImage());

        AzureClient client = ac.getParameter(AzureClient.class);
        azureUtils.validateSubnetRules(client, stack.getNetwork());
        try {
            String region = ac.getCloudContext().getLocation().getRegion().value();
            Map<String, AzureDiskType> storageAccounts = azureStackView.getStorageAccounts();
            for (String name : storageAccounts.keySet()) {
                azureStorage.createStorage(ac, client, name, storageAccounts.get(name), resourceGroupName, region);
            }
            client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Invalid provisioning type: %s", stackName));
        }

        CloudResource cloudResource = new CloudResource.Builder().type(ResourceType.ARM_TEMPLATE).name(stackName).build();
        List<CloudResourceStatus> resources = check(ac, Collections.singletonList(cloudResource));
        LOGGER.debug("Launched resources: {}", resources);
        return resources;
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        String stackName = azureUtils.getStackName(authenticatedContext.getCloudContext());

        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case ARM_TEMPLATE:
                    LOGGER.info("Checking Azure group stack status of: {}", stackName);
                    try {
                        Deployment resourceGroupDeployment = client.getTemplateDeployment(stackName, stackName);
                        CloudResourceStatus templateResourceStatus = azureUtils.getTemplateStatus(resource, resourceGroupDeployment, client, stackName);
                        result.add(templateResourceStatus);
                    } catch (CloudException e) {
                        if (e.getResponse().code() == AzureConstants.NOT_FOUND) {
                            result.add(new CloudResourceStatus(resource, ResourceStatus.DELETED));
                        }
                    } catch (Exception e) {
                        throw new CloudConnectorException(String.format("Invalid resource exception: %s", e.getMessage()), e);
                    }
                    break;
                default:
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }
        return result;
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        for (CloudResource resource : resources) {
            try {
                client.deleteResourceGroup(resource.getName());

                if (azureStorage.isPersistentStorage(azureStorage.getPersistentStorageName(stack.getParameters()))) {
                    CloudContext cloudCtx = authenticatedContext.getCloudContext();
                    String imageStorageName = azureStorage.getImageStorageName(new AzureCredentialView(authenticatedContext.getCloudCredential()), cloudCtx,
                            azureStorage.getPersistentStorageName(stack.getParameters()), azureStorage.getArmAttachedStorageOption(stack.getParameters()));
                    String imageResourceGroupName = azureStorage.getImageResourceGroupName(cloudCtx, stack.getParameters());
                    String diskContainer = azureStorage.getDiskContainerName(cloudCtx);
                    deleteContainer(client, imageResourceGroupName, imageStorageName, diskContainer);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not delete resource group: %s", resource.getName()), e);
            }
        }
        return check(authenticatedContext, resources);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        AzureCredentialView azureCredentialView = new AzureCredentialView(authenticatedContext.getCloudCredential());

        String stackName = azureUtils.getStackName(authenticatedContext.getCloudContext());
        AzureStackView azureStackView = getAzureStack(azureCredentialView, authenticatedContext.getCloudContext(), stack);
        String template = azureTemplateBuilder.build(stackName, azureCredentialView, azureStackView, authenticatedContext.getCloudContext(), stack);
        String parameters = azureTemplateBuilder.buildParameters(authenticatedContext.getCloudCredential(), stack.getNetwork(), stack.getImage());
        String resourceGroupName = azureUtils.getResourceGroupName(authenticatedContext.getCloudContext());

        try {
            String region = authenticatedContext.getCloudContext().getLocation().getRegion().value();
            Map<String, AzureDiskType> storageAccounts = azureStackView.getStorageAccounts();
            for (String name : storageAccounts.keySet()) {
                azureStorage.createStorage(authenticatedContext, client, name, storageAccounts.get(name), resourceGroupName, region);
            }
            client.createTemplateDeployment(stackName, stackName, template, parameters);
            List<CloudResourceStatus> check = new ArrayList<>();
            check.add(new CloudResourceStatus(resources.get(0), ResourceStatus.IN_PROGRESS));
            return check;
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not upscale: %s", stackName), e);
        }
    }

    @Override
    public Map<String, Map<String, Object>> collectResourcesToRemove(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources,
            List<CloudInstance> vms) {
        AzureClient client = ac.getParameter(AzureClient.class);
        AzureCredentialView azureCredentialView = new AzureCredentialView(ac.getCloudCredential());
        String stackName = azureUtils.getStackName(ac.getCloudContext());
        NetworkInterfaces runningNetworkInterfaces = null;
        double nodeNumber = stack.getGroups().stream().mapToInt(g -> g.getInstances().size()).sum();
        if (nodeNumber / vms.size() < PUBLIC_ADDRESS_BATCH_RATIO) {
            try {
                runningNetworkInterfaces = client.getNetworkInterfaces(stackName);
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not downscale: %s", stackName), e);
            }
        }
        Map<String, Map<String, Object>> resp = new HashMap<>();
        for (CloudInstance instance : vms) {
            resp.put(instance.getInstanceId(), collectInstanceResources(ac, stack, client, azureCredentialView, stackName, runningNetworkInterfaces,
                    instance));
        }
        return resp;
    }

    private Map<String, Object> collectInstanceResources(AuthenticatedContext ac, CloudStack stack, AzureClient client, AzureCredentialView azureCredentialView,
            String stackName, NetworkInterfaces runningNetworkInterfaces, CloudInstance instance) {
        String instanceId = instance.getInstanceId();
        Long privateId = instance.getTemplate().getPrivateId();
        AzureDiskType azureDiskType = AzureDiskType.getByValue(instance.getTemplate().getVolumeType());
        String attachedDiskStorageName = azureStorage.getAttachedDiskStorageName(azureStorage.getArmAttachedStorageOption(stack.getParameters()),
                azureCredentialView, privateId, ac.getCloudContext(), azureDiskType);
        Map<String, Object> resourcesToScale = new HashMap<>();
        resourcesToScale.put(ATTACHED_DISK_STORAGE_NAME, attachedDiskStorageName);
        try {
            VirtualMachine virtualMachine = client.getVirtualMachine(stackName, instanceId);
            List<String> networkInterfaceIds = virtualMachine.networkInterfaceIds();
            List<String> networkInterfacesNames = new ArrayList<>();
            List<String> publicIpAddressNames = new ArrayList<>();
            for (String interfaceId : networkInterfaceIds) {
                NetworkInterface networkInterface = client.getNetworkInterfaceById(interfaceId);
                String interfaceName = networkInterface.name();
                networkInterfacesNames.add(interfaceName);
                Set<String> ipNames = new HashSet<>();
                for (NicIpConfiguration ipConfiguration : networkInterface.ipConfigurations().values()) {
                    if (ipConfiguration.getPublicIpAddress() != null && ipConfiguration.getPublicIpAddress().name() != null) {
                        ipNames.add(ipConfiguration.getPublicIpAddress().name());
                    }
                }
                publicIpAddressNames.addAll(ipNames);
            }
            resourcesToScale.put(NETWORK_INTERFACES_NAMES, networkInterfacesNames);
            resourcesToScale.put(PUBLIC_ADDRESS_NAME, publicIpAddressNames);

            StorageProfile storageProfile = virtualMachine.storageProfile();
            List<DataDisk> dataDisks = storageProfile.dataDisks();

            List<String> storageProfileDiskNames = new ArrayList<>();
            for (DataDisk datadisk : dataDisks) {
                VirtualHardDisk vhd = datadisk.vhd();
                storageProfileDiskNames.add(getNameFromConnectionString(vhd.uri()));
            }
            OSDisk osDisk = storageProfile.osDisk();
            VirtualHardDisk vhd = osDisk.vhd();
            storageProfileDiskNames.add(getNameFromConnectionString(vhd.uri()));
            resourcesToScale.put(STORAGE_PROFILE_DISK_NAMES, storageProfileDiskNames);
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not downscale: %s", stackName), e);
        }
        return resourcesToScale;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            Map<String, Map<String, Object>> resourcesToRemove) {
        AzureClient client = ac.getParameter(AzureClient.class);
        String stackName = azureUtils.getStackName(ac.getCloudContext());
        String resourceGroupName = azureUtils.getResourceGroupName(ac.getCloudContext());
        String diskContainer = azureStorage.getDiskContainerName(ac.getCloudContext());
        for (CloudInstance instance : vms) {
            String instanceId = instance.getInstanceId();
            Map<String, Object> instanceResources = resourcesToRemove.get(instanceId);
            try {
                deallocateVirtualMachine(ac, client, stackName, instanceId);
                deleteVirtualMachine(ac, client, stackName, instanceId);
                deleteNetworkInterfaces(ac, client, stackName, (List<String>) instanceResources.get(NETWORK_INTERFACES_NAMES));
                deletePublicIps(ac, client, stackName, (List<String>) instanceResources.get(PUBLIC_ADDRESS_NAME));
                deleteDisk((List<String>) instanceResources.get(STORAGE_PROFILE_DISK_NAMES), client, resourceGroupName,
                        (String) instanceResources.get(ATTACHED_DISK_STORAGE_NAME), diskContainer);
                if (azureStorage.getArmAttachedStorageOption(stack.getParameters()) == ArmAttachedStorageOption.PER_VM) {
                    azureStorage.deleteStorage(ac, client, (String) instanceResources.get(ATTACHED_DISK_STORAGE_NAME), resourceGroupName);
                }
            } catch (CloudConnectorException e) {
                throw e;
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Failed to cleanup resources after downscale: %s", stackName), e);
            }
        }
        return check(ac, resources);
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingDoesNotSupportedException {
        return azureTemplateBuilder.getTemplateString();
    }

    private AzureStackView getAzureStack(AzureCredentialView azureCredentialView, CloudContext cloudContext, CloudStack cloudStack) {
        return new AzureStackView(cloudContext.getName(), stackNamePrefixLength, cloudStack.getGroups(), new AzureStorageView(azureCredentialView, cloudContext,
                azureStorage, azureStorage.getArmAttachedStorageOption(cloudStack.getParameters())));
    }

    private void deleteContainer(AzureClient azureClient, String resourceGroup, String storageName, String
            container) {
        try {
            azureClient.deleteContainerInStorage(resourceGroup, storageName, container);
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not delete container: %s", container), e);
        }
    }

    private void deleteDisk(List<String> storageProfileDiskNames, AzureClient azureClient, String resourceGroup, String storageName, String container) {
        for (String storageProfileDiskName : storageProfileDiskNames) {
            try {
                azureClient.deleteBlobInStorageContainer(resourceGroup, storageName, container, storageProfileDiskName);
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not delete blob: %s", storageProfileDiskName), e);
            }
        }
    }

    private void deleteNetworkInterfaces(AuthenticatedContext authenticatedContext, AzureClient client, String stackName, List<String> networkInterfacesNames)
            throws CloudConnectorException {
        for (String networkInterfacesName : networkInterfacesNames) {
            try {
                client.deleteNetworkInterface(stackName, networkInterfacesName);
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not delete network interface: %s", networkInterfacesName), e);
            }
        }
    }

    private void deletePublicIps(AuthenticatedContext authenticatedContext, AzureClient client, String stackName, List<String> publicIpNames) {
        for (String publicIpName : publicIpNames) {
            try {
                client.deletePublicIpAddressByName(stackName, publicIpName);
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not delete public IP address: %s", publicIpName), e);
            }
        }
    }

    private void deleteVirtualMachine(AuthenticatedContext authenticatedContext, AzureClient client, String stackName, String privateInstanceId)
            throws CloudConnectorException {
        try {
            client.deleteVirtualMachine(stackName, privateInstanceId);
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not delete virtual machine: %s", privateInstanceId), e);
        }
    }

    private void deallocateVirtualMachine(AuthenticatedContext authenticatedContext, AzureClient client, String stackName, String privateInstanceId)
            throws CloudConnectorException {
        try {
            client.deallocateVirtualMachine(stackName, privateInstanceId);
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not deallocate machine: %s", privateInstanceId), e);
        }
    }

    private String getNameFromConnectionString(String connection) {
        return connection.split("/")[connection.split("/").length - 1];
    }

}
