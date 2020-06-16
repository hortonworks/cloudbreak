package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.OSDisk;
import com.microsoft.azure.management.compute.StorageProfile;
import com.microsoft.azure.management.compute.VirtualHardDisk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NicIPConfiguration;
import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDatabaseResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.upscale.AzureUpscaleService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureResourceConnector implements ResourceConnector<Map<String, Map<String, Object>>> {

    public static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceConnector.class);

    private static final String NETWORK_INTERFACES_NAMES = "NETWORK_INTERFACES_NAMES";

    private static final String STORAGE_PROFILE_DISK_NAMES = "STORAGE_PROFILE_DISK_NAMES";

    private static final String ATTACHED_DISK_STORAGE_NAME = "ATTACHED_DISK_STORAGE_NAME";

    private static final String MANAGED_DISK_IDS = "MANAGED_DISK_IDS";

    private static final String PUBLIC_ADDRESS_NAME = "PUBLIC_ADDRESS_NAME";

    @Inject
    private AzureTemplateBuilder azureTemplateBuilder;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private AzureVirtualMachineService azureVirtualMachineService;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private AzureComputeResourceService azureComputeResourceService;

    @Inject
    private AzureDatabaseResourceService azureDatabaseResourceService;

    @Inject
    private AzureUpscaleService azureUpscaleService;

    @Inject
    private AzureStackViewProvider azureStackViewProvider;

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    @Inject
    private AzureDownscaleService azureDownscaleService;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentType adjustmentType, Long threshold) {
        AzureCredentialView azureCredentialView = new AzureCredentialView(ac.getCloudCredential());
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        AzureClient client = ac.getParameter(AzureClient.class);

        AzureStackView azureStackView = azureStackViewProvider.getAzureStack(azureCredentialView, stack, client, ac);

        String customImageId = azureStorage.getCustomImageId(client, ac, stack);
        String template = azureTemplateBuilder.build(stackName, customImageId, azureCredentialView, azureStackView,
                cloudContext, stack, AzureInstanceTemplateOperation.PROVISION);

        String parameters = azureTemplateBuilder.buildParameters(ac.getCloudCredential(), stack.getNetwork(), stack.getImage());
        Boolean encrytionNeeded = azureStorage.isEncrytionNeeded(stack.getParameters());

        try {
            String region = cloudContext.getLocation().getRegion().value();
            if (AzureUtils.hasUnmanagedDisk(stack)) {
                Map<String, AzureDiskType> storageAccounts = azureStackView.getStorageAccounts();
                for (Entry<String, AzureDiskType> entry : storageAccounts.entrySet()) {
                    azureStorage.createStorage(client, entry.getKey(), entry.getValue(), resourceGroupName, region, encrytionNeeded, stack.getTags());
                }
            }
            if (!client.templateDeploymentExists(resourceGroupName, stackName)) {
                Deployment templateDeployment = client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
                LOGGER.debug("Created template deployment for launch: {}", templateDeployment.exportTemplate().template());

                List<CloudResource> cloudResources = azureCloudResourceService.getCloudResources(templateDeployment);
                azureCloudResourceService.saveCloudResources(notifier, cloudContext, cloudResources);

                String networkName = azureUtils.getCustomNetworkId(stack.getNetwork());
                List<String> subnetNameList = azureUtils.getCustomSubnetIds(stack.getNetwork());
                List<CloudResource> networkResources = azureCloudResourceService.collectAndSaveNetworkAndSubnet(
                        resourceGroupName, stackName, notifier, cloudContext, subnetNameList, networkName, client);
                List<CloudResource> instances = azureCloudResourceService.getInstanceCloudResources(
                        stackName, cloudResources, stack.getGroups(), resourceGroupName);
                azureComputeResourceService.buildComputeResourcesForLaunch(ac, stack, adjustmentType, threshold, instances, networkResources);
            }
        } catch (CloudException e) {
            LOGGER.info("Provisioning error, cloud exception happened: ", e);
            if (e.body() != null && e.body().details() != null) {
                String details = e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", "));
                throw new CloudConnectorException(String.format("Stack provisioning failed, status code %s, error message: %s, details: %s",
                        e.body().code(), e.body().message(), details));
            } else {
                throw new CloudConnectorException(String.format("Stack provisioning failed: '%s', please go to Azure Portal for detailed message", e));
            }
        } catch (Exception e) {
            LOGGER.warn("Provisioning error:", e);
            throw new CloudConnectorException(String.format("Error in provisioning stack %s: %s", stackName, e.getMessage()));
        }

        CloudResource cloudResource = new Builder()
                .type(ResourceType.ARM_TEMPLATE)
                .name(resourceGroupName)
                .build();
        List<CloudResourceStatus> resources = check(ac, Collections.singletonList(cloudResource));
        LOGGER.debug("Launched resources: {}", resources);
        return resources;
    }

    @Override
    public List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier) {
        return azureDatabaseResourceService.buildDatabaseResourcesForLaunch(authenticatedContext, stack, persistenceNotifier);
    }

    @Override
    public void startDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        throw new UnsupportedOperationException("Database server start operation is not supported for " + getClass().getName());
    }

    @Override
    public void stopDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) {
        throw new UnsupportedOperationException("Database server stop operation is not supported for " + getClass().getName());
    }

    @Override
    public ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception {
        return azureDatabaseResourceService.getDatabaseServerStatus(authenticatedContext, stack);
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        String stackName = azureUtils.getStackName(authenticatedContext.getCloudContext());

        for (CloudResource resource : resources) {
            ResourceType resourceType = resource.getType();
            if (resourceType == ResourceType.ARM_TEMPLATE) {
                LOGGER.debug("Checking Azure stack status of: {}", stackName);
                checkTemplateDeployment(result, client, stackName, resource);
            } else {
                if (!resourceType.name().startsWith("AZURE")) {
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resourceType));
                }
            }
        }
        return result;
    }

    private void checkTemplateDeployment(List<CloudResourceStatus> result, AzureClient client, String stackName, CloudResource resource) {
        try {
            String resourceGroupName = resource.getName();
            CloudResourceStatus templateResourceStatus;
            if (client.templateDeploymentExists(resourceGroupName, stackName)) {
                Deployment resourceGroupDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
                templateResourceStatus = azureUtils.getTemplateStatus(resource, resourceGroupDeployment, client, stackName);
            } else {
                templateResourceStatus = new CloudResourceStatus(resource, ResourceStatus.DELETED);
            }
            result.add(templateResourceStatus);
        } catch (CloudException e) {
            if (e.response().code() == AzureConstants.NOT_FOUND) {
                result.add(new CloudResourceStatus(resource, ResourceStatus.DELETED));
            } else {
                throw new CloudConnectorException(e.body().message(), e);
            }
        } catch (RuntimeException e) {
            throw new CloudConnectorException(String.format("Invalid resource exception: %s", e.getMessage()), e);
        }
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        AzureClient client = ac.getParameter(AzureClient.class);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), stack);
        try {
            try {
                retryService.testWith2SecDelayMax5Times(() -> {
                    if (!client.resourceGroupExists(resourceGroupName)) {
                        throw new ActionFailedException("Resource group not exists");
                    }
                    return true;
                });
                client.deleteResourceGroup(resourceGroupName);
            } catch (ActionFailedException ignored) {
                LOGGER.debug("Resource group not found with name: {}", resourceGroupName);
            }
            if (azureStorage.isPersistentStorage(azureStorage.getPersistentStorageName(stack))) {
                CloudContext cloudCtx = ac.getCloudContext();
                AzureCredentialView azureCredentialView = new AzureCredentialView(ac.getCloudCredential());

                String imageStorageName = azureStorage.getImageStorageName(azureCredentialView, cloudCtx, stack);
                String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(cloudCtx, stack);
                String diskContainer = azureStorage.getDiskContainerName(cloudCtx);
                deleteContainer(client, imageResourceGroupName, imageStorageName, diskContainer);
            }
        } catch (CloudException e) {
            if (e.response().code() != AzureConstants.NOT_FOUND) {
                throw new CloudConnectorException(String.format("Could not delete resource group: %s", resourceGroupName), e);
            } else {
                return check(ac, Collections.emptyList());
            }
        }
        return check(ac, resources);
    }

    @Override
    public List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            List<CloudResource> resources, PersistenceNotifier persistenceNotifier, boolean force) {
        return azureDatabaseResourceService.terminateDatabaseServer(authenticatedContext, stack, resources, force, persistenceNotifier);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        AzureClient client = ac.getParameter(AzureClient.class);
        AzureStackView azureStackView = azureStackViewProvider.getAzureStack(new AzureCredentialView(ac.getCloudCredential()), stack, client, ac);
        return azureUpscaleService.upscale(ac, stack, resources, azureStackView, client);
    }

    @Override
    public Map<String, Map<String, Object>> collectResourcesToRemove(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources,
            List<CloudInstance> vms) {
        AzureClient client = ac.getParameter(AzureClient.class);
        AzureCredentialView azureCredentialView = new AzureCredentialView(ac.getCloudCredential());
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), stack);
        Map<String, Map<String, Object>> resp = new HashMap<>(vms.size());
        for (CloudInstance instance : vms) {
            // TODO: it should be async!
            resp.put(instance.getInstanceId(), collectInstanceResourcesToRemove(ac, stack, client, azureCredentialView, resourceGroupName, instance));
        }
        return resp;
    }

    private Map<String, Object> collectInstanceResourcesToRemove(AuthenticatedContext ac, CloudStack stack, AzureClient client,
            AzureCredentialView azureCredentialView, String resourceGroupName, CloudInstance instance) {
        String instanceId = instance.getInstanceId();
        Long privateId = instance.getTemplate().getPrivateId();
        Map<String, Object> resourcesToRemove = new HashMap<>();
        addAttachedDiskStorageForRemoval(resourcesToRemove, ac, stack, azureCredentialView, instance, privateId);
        try {
            if (instanceId != null) {
                VirtualMachine virtualMachine = client.getVirtualMachine(resourceGroupName, instanceId);
                if (virtualMachine != null) {
                    List<String> networkInterfaceIds = virtualMachine.networkInterfaceIds();
                    List<String> networkInterfacesNames = new ArrayList<>();
                    List<String> publicIpAddressNames = new ArrayList<>();
                    for (String interfaceId : networkInterfaceIds) {
                        NetworkInterface networkInterface = client.getNetworkInterfaceById(interfaceId);
                        String interfaceName = networkInterface.name();
                        networkInterfacesNames.add(interfaceName);
                        Collection<String> ipNames = new HashSet<>();
                        for (NicIPConfiguration ipConfiguration : networkInterface.ipConfigurations().values()) {
                            if (ipConfiguration.publicIPAddressId() != null && ipConfiguration.getPublicIPAddress().name() != null) {
                                ipNames.add(ipConfiguration.getPublicIPAddress().name());
                            }
                        }
                        publicIpAddressNames.addAll(ipNames);
                    }
                    resourcesToRemove.put(NETWORK_INTERFACES_NAMES, networkInterfacesNames);
                    resourcesToRemove.put(PUBLIC_ADDRESS_NAME, publicIpAddressNames);

                    collectRemovableDisks(resourcesToRemove, virtualMachine);
                }
            }
        } catch (CloudException e) {
            if (e.response().code() != AzureConstants.NOT_FOUND) {
                throw new CloudConnectorException(e.body().message(), e);
            }
        } catch (RuntimeException e) {
            throw new CloudConnectorException("can't collect instance resources", e);
        }
        return resourcesToRemove;
    }

    private void addAttachedDiskStorageForRemoval(Map<String, Object> resourcesToRemove, AuthenticatedContext ac, CloudStack stack,
            AzureCredentialView azureCredentialView, CloudInstance instance, Long privateId) {
        if (!instance.getTemplate().getVolumes().isEmpty()) {
            AzureDiskType azureDiskType = AzureDiskType.getByValue(instance.getTemplate().getVolumes().get(0).getType());
            String attachedDiskStorageName = azureStorage.getAttachedDiskStorageName(azureStorage.getArmAttachedStorageOption(stack.getParameters()),
                    azureCredentialView, privateId, ac.getCloudContext(), azureDiskType);
            resourcesToRemove.put(ATTACHED_DISK_STORAGE_NAME, attachedDiskStorageName);
        }
    }

    private void collectRemovableDisks(Map<String, Object> resourcesToRemove, VirtualMachine virtualMachine) {
        StorageProfile storageProfile = virtualMachine.storageProfile();
        Collection<String> storageProfileDiskNames = new ArrayList<>();
        Collection<String> managedDiskIds = new ArrayList<>();

        // Handle OS disks as ephemeral
        OSDisk osDisk = storageProfile.osDisk();
        if (osDisk.vhd() != null) {
            VirtualHardDisk vhd = osDisk.vhd();
            storageProfileDiskNames.add(getNameFromConnectionString(vhd.uri()));
        } else {
            managedDiskIds.add(osDisk.managedDisk().id());
        }
        resourcesToRemove.put(STORAGE_PROFILE_DISK_NAMES, storageProfileDiskNames);
        resourcesToRemove.put(MANAGED_DISK_IDS, managedDiskIds);
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            Map<String, Map<String, Object>> resourcesToRemove) {
        return azureDownscaleService.downscale(ac, stack, resources, vms, resourcesToRemove);
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() {
        return azureTemplateBuilder.getTemplateString();
    }

    @Override
    public String getDBStackTemplate() {
        return azureTemplateBuilder.getDBTemplateString();
    }

    private Map<String, String> getCustomImageNamePerInstance(AuthenticatedContext ac, CloudStack cloudStack) {
        AzureClient client = ac.getParameter(AzureClient.class);
        Map<String, String> imageNameMap = new HashMap<>();
        Map<String, String> customImageNamePerInstance = new HashMap<>();
        for (Group group : cloudStack.getGroups()) {
            for (CloudInstance instance : group.getInstances()) {
                String imageId = instance.getTemplate().getImageId();
                if (StringUtils.isNotBlank(imageId)) {
                    String imageCustomName = imageNameMap.computeIfAbsent(imageId, s -> azureStorage.getCustomImageId(client, ac, cloudStack, imageId));
                    customImageNamePerInstance.put(instance.getInstanceId(), imageCustomName);
                }
            }
        }
        return customImageNamePerInstance;
    }

    private void deleteContainer(AzureClient azureClient, String resourceGroup, String storageName, String container) {
        try {
            azureClient.deleteContainerInStorage(resourceGroup, storageName, container);
        } catch (CloudException e) {
            if (e.response().code() != AzureConstants.NOT_FOUND) {
                throw new CloudConnectorException(String.format("Could not delete container: %s", container), e);
            } else {
                LOGGER.debug("Container not found: resourcegroup={}, storagename={}, container={}",
                        resourceGroup, storageName, container);
            }
        }
    }

    private String getNameFromConnectionString(String connection) {
        return connection.split("/")[connection.split("/").length - 1];
    }

}
