package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.arm.context.NetworkInterfaceCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.context.ResourceGroupCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmStackView;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmStorageView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

import groovyx.net.http.HttpResponseException;

@Service
public class ArmResourceConnector implements ResourceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArmResourceConnector.class);

    @Inject
    private ArmClient armClient;
    @Inject
    private ArmTemplateBuilder armTemplateBuilder;
    @Inject
    private ArmUtils armUtils;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private ArmPollTaskFactory armPollTaskFactory;
    @Inject
    private ArmStorage armStorage;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentType adjustmentType, Long threshold) {
        ArmCredentialView armCredentialView = new ArmCredentialView(ac.getCloudCredential());
        String stackName = armUtils.getStackName(ac.getCloudContext());
        String resourceGroupName = armUtils.getResourceGroupName(ac.getCloudContext());
        ArmStackView armStackView = getArmStack(armCredentialView, ac.getCloudContext(), stack);
        armUtils.validateStorageType(stack);
        String template = armTemplateBuilder.build(stackName, armCredentialView, armStackView, ac.getCloudContext(), stack);
        String parameters = armTemplateBuilder.buildParameters(ac.getCloudCredential(), stack.getNetwork(), stack.getImage());
        AzureRMClient client = armClient.getClient(ac.getCloudCredential());
        armUtils.validateSubnetRules(client, stack.getNetwork());
        try {
            String region = ac.getCloudContext().getLocation().getRegion().value();
            Map<String, ArmDiskType> storageAccounts = armStackView.getStorageAccounts();
            for (String name : storageAccounts.keySet()) {
                armStorage.createStorage(ac, client, name, storageAccounts.get(name), resourceGroupName, region);
            }
            client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
        } catch (HttpResponseException e) {
            throw new CloudConnectorException(String.format("Error occurred when creating stack: %s", e.getResponse().getData().toString()));
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
        AzureRMClient access = armClient.getClient(authenticatedContext.getCloudCredential());
        String stackName = armUtils.getStackName(authenticatedContext.getCloudContext());

        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case ARM_TEMPLATE:
                    LOGGER.info("Checking Arm group stack status of: {}", stackName);
                    try {
                        Map<String, Object> resourceGroup = access.getTemplateDeployment(stackName, stackName);
                        CloudResourceStatus templateResourceStatus = armUtils.templateStatus(resource, resourceGroup, access, stackName);
                        result.add(templateResourceStatus);
                    } catch (HttpResponseException e) {
                        if (e.getStatusCode() == NOT_FOUND) {
                            result.add(new CloudResourceStatus(resource, ResourceStatus.DELETED));
                        } else {
                            throw new CloudConnectorException(e.getResponse().getData().toString(), e);
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
        AzureRMClient azureRMClient = armClient.getClient(authenticatedContext.getCloudCredential());
        for (CloudResource resource : resources) {
            try {
                azureRMClient.deleteResourceGroup(resource.getName());
                PollTask<Boolean> task = armPollTaskFactory.newResourceGroupDeleteStatusCheckerTask(authenticatedContext, armClient,
                        new ResourceGroupCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()), resource.getName()));
                Boolean statePollerResult = task.call();
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(task);
                }
                if (armStorage.isPersistentStorage(armStorage.getPersistentStorageName(stack.getParameters()))) {
                    CloudContext cloudCtx = authenticatedContext.getCloudContext();
                    String imageStorageName = armStorage.getImageStorageName(new ArmCredentialView(authenticatedContext.getCloudCredential()), cloudCtx,
                            armStorage.getPersistentStorageName(stack.getParameters()), armStorage.getArmAttachedStorageOption(stack.getParameters()));
                    String imageResourceGroupName = armStorage.getImageResourceGroupName(cloudCtx, stack.getParameters());
                    String diskContainer = armStorage.getDiskContainerName(cloudCtx);
                    deleteContainer(azureRMClient, imageResourceGroupName, imageStorageName, diskContainer);
                }
            } catch (HttpResponseException e) {
                if (e.getStatusCode() != NOT_FOUND) {
                    throw new CloudConnectorException(e.getResponse().getData().toString(), e);
                } else {
                    return check(authenticatedContext, new ArrayList<>());
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
        AzureRMClient azureRMClient = armClient.getClient(authenticatedContext.getCloudCredential());
        ArmCredentialView armCredentialView = new ArmCredentialView(authenticatedContext.getCloudCredential());

        String stackName = armUtils.getStackName(authenticatedContext.getCloudContext());
        ArmStackView armStackView = getArmStack(armCredentialView, authenticatedContext.getCloudContext(), stack);
        String template = armTemplateBuilder.build(stackName, armCredentialView, armStackView, authenticatedContext.getCloudContext(), stack);
        String parameters = armTemplateBuilder.buildParameters(authenticatedContext.getCloudCredential(), stack.getNetwork(), stack.getImage());
        String resourceGroupName = armUtils.getResourceGroupName(authenticatedContext.getCloudContext());

        try {
            String region = authenticatedContext.getCloudContext().getLocation().getRegion().value();
            Map<String, ArmDiskType> storageAccounts = armStackView.getStorageAccounts();
            for (String name : storageAccounts.keySet()) {
                armStorage.createStorage(authenticatedContext, azureRMClient, name, storageAccounts.get(name), resourceGroupName, region);
            }
            azureRMClient.createTemplateDeployment(stackName, stackName, template, parameters);
            List<CloudResourceStatus> check = new ArrayList<>();
            check.add(new CloudResourceStatus(resources.get(0), ResourceStatus.IN_PROGRESS));
            return check;
        } catch (HttpResponseException e) {
            throw new CloudConnectorException(e.getResponse().getData().toString(), e);
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not upscale: %s", stackName), e);
        }
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        AzureRMClient client = armClient.getClient(ac.getCloudCredential());
        ArmCredentialView armCredentialView = new ArmCredentialView(ac.getCloudCredential());
        String stackName = armUtils.getStackName(ac.getCloudContext());

        String resourceGroupName = armUtils.getResourceGroupName(ac.getCloudContext());
        String diskContainer = armStorage.getDiskContainerName(ac.getCloudContext());

        for (CloudInstance instance : vms) {
            List<String> networkInterfacesNames = new ArrayList<>();
            List<String> storageProfileDiskNames = new ArrayList<>();
            String instanceId = instance.getInstanceId();
            Long privateId = instance.getTemplate().getPrivateId();
            ArmDiskType armDiskType = ArmDiskType.getByValue(instance.getTemplate().getVolumeType());
            String attachedDiskStorageName = armStorage.getAttachedDiskStorageName(armStorage.getArmAttachedStorageOption(stack.getParameters()),
                    armCredentialView, privateId, ac.getCloudContext(), armDiskType);
            try {
                Map<String, Object> virtualMachine = client.getVirtualMachine(stackName, instanceId);

                Map properties = (Map) virtualMachine.get("properties");

                Map networkProfile = (Map) properties.get("networkProfile");

                List<Map> networkInterfaces = (List<Map>) networkProfile.get("networkInterfaces");
                for (Map networkInterface : networkInterfaces) {
                    networkInterfacesNames.add(getNameFromConnectionString(networkInterface.get("id").toString()));
                }

                Map storageProfile = (Map) properties.get("storageProfile");

                Map osDisk = (Map) storageProfile.get("osDisk");
                List<Map> dataDisks = (List<Map>) storageProfile.get("dataDisks");

                for (Map datadisk : dataDisks) {
                    Map vhds = (Map) datadisk.get("vhd");
                    storageProfileDiskNames.add(getNameFromConnectionString(vhds.get("uri").toString()));
                }
                Map vhds = (Map) osDisk.get("vhd");
                storageProfileDiskNames.add(getNameFromConnectionString(vhds.get("uri").toString()));
            } catch (HttpResponseException e) {
                if (e.getStatusCode() != NOT_FOUND) {
                    throw new CloudConnectorException(e.getResponse().getData().toString(), e);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not downscale: %s", stackName), e);
            }
            try {
                deallocateVirtualMachine(ac, client, stackName, instanceId);
                deleteVirtualMachine(ac, client, stackName, instanceId);
                deleteNetworkInterfaces(ac, client, stackName, networkInterfacesNames);
                deleteDisk(storageProfileDiskNames, client, resourceGroupName, attachedDiskStorageName, diskContainer);
                if (armStorage.getArmAttachedStorageOption(stack.getParameters()) == ArmAttachedStorageOption.PER_VM) {
                    armStorage.deleteStorage(ac, client, attachedDiskStorageName, resourceGroupName);
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

    private ArmStackView getArmStack(ArmCredentialView armCredentialView, CloudContext cloudContext, CloudStack cloudStack) {
        return new ArmStackView(cloudStack.getGroups(), new ArmStorageView(armCredentialView, cloudContext, armStorage,
                armStorage.getArmAttachedStorageOption(cloudStack.getParameters())));
    }

    private void deleteContainer(AzureRMClient azureRMClient, String resourceGroup, String storageName, String
            container) {
        try {
            azureRMClient.deleteContainerInStorage(resourceGroup, storageName, container);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != NOT_FOUND) {
                throw new CloudConnectorException(e.getResponse().getData().toString());
            }
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not delete container: %s", container), e);
        }
    }

    private void deleteDisk(List<String> storageProfileDiskNames, AzureRMClient azureRMClient, String resourceGroup, String storageName, String container) {
        for (String storageProfileDiskName : storageProfileDiskNames) {
            try {
                azureRMClient.deleteBlobInStorageContainer(resourceGroup, storageName, container, storageProfileDiskName);
            } catch (HttpResponseException e) {
                if (e.getStatusCode() != NOT_FOUND) {
                    throw new CloudConnectorException(e.getResponse().getData().toString());
                }
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not delete blob: %s", storageProfileDiskName), e);
            }
        }
    }

    private void deleteNetworkInterfaces(AuthenticatedContext authenticatedContext, AzureRMClient client, String stackName, List<String> networkInterfacesNames)
            throws CloudConnectorException {
        for (String networkInterfacesName : networkInterfacesNames) {
            try {
                client.deleteNetworkInterface(stackName, networkInterfacesName);
                PollTask<Boolean> task = armPollTaskFactory.newNetworkInterfaceDeleteStatusCheckerTask(authenticatedContext, armClient,
                        new NetworkInterfaceCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()),
                                stackName, networkInterfacesName));

                syncPollingScheduler.schedule(task);

            } catch (HttpResponseException e) {
                if (e.getStatusCode() != NOT_FOUND) {
                    throw new CloudConnectorException(e.getResponse().getData().toString(), e);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not delete network interface: %s", networkInterfacesName), e);
            }
        }
    }

    private void deleteVirtualMachine(AuthenticatedContext authenticatedContext, AzureRMClient client, String stackName, String privateInstanceId)
            throws CloudConnectorException {
        try {
            client.deleteVirtualMachine(stackName, privateInstanceId);
            PollTask<Boolean> task = armPollTaskFactory.newVirtualMachineDeleteStatusCheckerTask(authenticatedContext, armClient,
                    new VirtualMachineCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()),
                            stackName, privateInstanceId));
            syncPollingScheduler.schedule(task);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != NOT_FOUND) {
                throw new CloudConnectorException(e.getResponse().getData().toString(), e);
            }
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not delete virtual machine: %s", privateInstanceId), e);
        }
    }

    private void deallocateVirtualMachine(AuthenticatedContext authenticatedContext, AzureRMClient client, String stackName, String privateInstanceId)
            throws CloudConnectorException {
        try {
            client.deallocateVirtualMachine(stackName, privateInstanceId);
            PollTask<Boolean> task = armPollTaskFactory.newVirtualMachineStatusCheckerTask(authenticatedContext, armClient,
                    new VirtualMachineCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()),
                            stackName, privateInstanceId, "Succeeded"));
            syncPollingScheduler.schedule(task);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != NOT_FOUND) {
                throw new CloudConnectorException(e.getResponse().getData().toString(), e);
            }
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Could not deallocate machine: %s", privateInstanceId), e);
        }
    }

    private String getNameFromConnectionString(String connection) {
        return connection.split("/")[connection.split("/").length - 1];
    }

}
