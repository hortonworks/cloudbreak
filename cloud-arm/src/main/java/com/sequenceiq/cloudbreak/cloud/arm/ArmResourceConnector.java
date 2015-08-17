package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmTemplateUtils.NOT_FOUND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.arm.poller.ArmNetworkInterfaceDeleteStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.arm.poller.ArmResourceGroupDeleteStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.arm.poller.ArmVirtualMachineDeleteStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.arm.poller.ArmVirtualMachineStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.arm.poller.context.NetworkInterfaceCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.poller.context.ResourceGroupCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.poller.context.VirtualMachineCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.BooleanResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.ResourceType;

import groovyx.net.http.HttpResponseException;
import reactor.rx.Promise;

@Service
public class ArmResourceConnector implements ResourceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArmResourceConnector.class);

    @Inject
    private ArmClient armClient;
    @Inject
    private ArmTemplateBuilder armTemplateBuilder;
    @Inject
    private ArmTemplateUtils armTemplateUtils;
    @Inject
    private SyncPollingScheduler<BooleanResult> syncPollingScheduler;
    @Inject
    private PollTaskFactory statusCheckFactory;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier notifier) {
        String stackName = armTemplateUtils.getStackName(authenticatedContext.getCloudContext());
        String template = armTemplateBuilder.build(stackName, authenticatedContext.getCloudCredential(), stack);
        String parameters = armTemplateBuilder.buildParameters(authenticatedContext.getCloudCredential(), stack.getNetwork(), stack.getImage());

        AzureRMClient access = armClient.createAccess(authenticatedContext.getCloudCredential());
        try {
            access.createWholeStack(CloudRegion.valueOf(stack.getRegion()).value(), stackName, stackName, template, parameters);
        } catch (HttpResponseException e) {
            throw new CloudConnectorException(String.format("Error occured when creating stack: %s", e.getResponse().getData().toString()));
        } catch (Exception e) {
            throw new CloudConnectorException(String.format("Invalid provisiong type: %s", stackName));
        }

        CloudResource cloudResource = new CloudResource(ResourceType.ARM_TEMPLATE, stackName);

        Promise<ResourcePersisted> promise = notifier.notifyAllocation(cloudResource, authenticatedContext.getCloudContext());
        try {
            promise.await();
        } catch (Exception e) {
            //Rollback
            terminate(authenticatedContext, stack, Arrays.asList(cloudResource));
        }

        List<CloudResourceStatus> resources = check(authenticatedContext, Arrays.asList(cloudResource));
        LOGGER.debug("Launched resources: {}", resources);
        return resources;
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        AzureRMClient access = armClient.createAccess(authenticatedContext.getCloudCredential());
        String stackName = armTemplateUtils.getStackName(authenticatedContext.getCloudContext());

        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case ARM_TEMPLATE:
                    LOGGER.info("Checking Arm group stack status of: {}", stackName);
                    try {
                        Map<String, Object> resourceGroup = access.getTemplateDeployment(stackName, stackName);
                        CloudResourceStatus templateResourceStatus = armTemplateUtils.templateStatus(resource, resourceGroup);
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
        AzureRMClient azureRMClient = armClient.createAccess(authenticatedContext.getCloudCredential());
        String osStorageName = armClient.getStorageName(authenticatedContext.getCloudCredential(), stack.getRegion());
        String storageGroup = armClient.getStorageGroup(authenticatedContext.getCloudCredential(), stack.getRegion());
        for (CloudResource resource : resources) {
            List<String> storageProfileDiskNames = new ArrayList<>();

            try {
                Map<String, Object> virtualMachines = azureRMClient.getVirtualMachines(resource.getName());
                List<Map> values = (List<Map>) virtualMachines.get("value");
                for (Map value : values) {
                    Map properties = (Map) value.get("properties");
                    Map storageProfile = (Map) properties.get("storageProfile");

                    Map osDisk = (Map) storageProfile.get("osDisk");
                    List<Map> dataDisks = (List<Map>) storageProfile.get("dataDisks");

                    for (Map datadisk : dataDisks) {
                        Map vhds = (Map) datadisk.get("vhd");
                        storageProfileDiskNames.add(getNameFromConnectionString(vhds.get("uri").toString()));
                    }
                    Map vhds = (Map) osDisk.get("vhd");
                    storageProfileDiskNames.add(getNameFromConnectionString(vhds.get("uri").toString()));
                }
                azureRMClient.deleteResourceGroup(resource.getName());
                PollTask<BooleanResult> task = statusCheckFactory.newPollBooleanTerminationTask(authenticatedContext,
                        new ArmResourceGroupDeleteStatusCheckerTask(armClient, new ResourceGroupCheckerContext(
                                new ArmCredentialView(authenticatedContext.getCloudCredential()), resource.getName())));
                BooleanResult statePollerResult = task.call();
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(task);
                }
            } catch (HttpResponseException e) {
                if (e.getStatusCode() != NOT_FOUND) {
                    throw new CloudConnectorException(e.getResponse().getData().toString(), e);
                }
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not delete resource group: %s", resource.getName()), e);
            }
            deleteDisk(storageProfileDiskNames, azureRMClient, osStorageName, storageGroup);
        }
        return check(authenticatedContext, resources);
    }

    private void deleteDisk(List<String> storageProfileDiskNames, AzureRMClient azureRMClient, String osStorageName, String storageGroup) {
        for (String storageProfileDiskName : storageProfileDiskNames) {
            try {
                azureRMClient.deleteBlobInStorageContainer(storageGroup, osStorageName, ArmSetup.VHDS, storageProfileDiskName);
            } catch (HttpResponseException e) {
                if (e.getStatusCode() != NOT_FOUND) {
                    throw new CloudConnectorException(e.getResponse().getData().toString());
                }
            } catch (Exception e) {
                throw new CloudConnectorException(String.format("Could not delete blob: %s", storageProfileDiskName), e);
            }
        }
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        AzureRMClient azureRMClient = armClient.createAccess(authenticatedContext.getCloudCredential());

        String stackName = armTemplateUtils.getStackName(authenticatedContext.getCloudContext());
        String template = armTemplateBuilder.build(stackName, authenticatedContext.getCloudCredential(), stack);
        String parameters = armTemplateBuilder.buildParameters(authenticatedContext.getCloudCredential(), stack.getNetwork(), stack.getImage());

        try {
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
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        AzureRMClient client = armClient.createAccess(auth.getCloudCredential());
        String stackName = armTemplateUtils.getStackName(auth.getCloudContext());
        String osStorageName = armClient.getStorageName(auth.getCloudCredential(), stack.getRegion());
        String storageGroup = armClient.getStorageGroup(auth.getCloudCredential(), stack.getRegion());

        for (CloudInstance instance : vms) {
            List<String> networkInterfacesNames = new ArrayList<>();
            List<String> storageProfileDiskNames = new ArrayList<>();
            String instanceId = instance.getInstanceId();

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
                deallocateVirtualMachine(auth, client, stackName, instanceId);
                deleteVirtualMachine(auth, client, stackName, instanceId);
                deleteNetworkInterfaces(auth, client, stackName, networkInterfacesNames);
                deleteDisk(storageProfileDiskNames, client, osStorageName, storageGroup);
            } catch (CloudConnectorException e) {
                throw e;
            }

        }
        return check(auth, resources);
    }

    private void deleteNetworkInterfaces(AuthenticatedContext authenticatedContext, AzureRMClient client, String stackName, List<String> networkInterfacesNames)
            throws CloudConnectorException {
        for (String networkInterfacesName : networkInterfacesNames) {
            try {
                client.deleteNetworkInterface(stackName, networkInterfacesName);
                BooleanResult statePollerResult = ResourcesStatePollerResults.transformToFalseBooleanResult(authenticatedContext.getCloudContext());
                PollTask<BooleanResult> task = statusCheckFactory.newPollBooleanStateTask(authenticatedContext,
                        new ArmNetworkInterfaceDeleteStatusCheckerTask(armClient,
                                new NetworkInterfaceCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()),
                                        stackName, networkInterfacesName)));
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(task);
                }
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
            BooleanResult statePollerResult = ResourcesStatePollerResults.transformToFalseBooleanResult(authenticatedContext.getCloudContext());
            PollTask<BooleanResult> task = statusCheckFactory.newPollBooleanStateTask(authenticatedContext,
                    new ArmVirtualMachineDeleteStatusCheckerTask(armClient,
                            new VirtualMachineCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()),
                                    stackName, privateInstanceId)));
            if (!task.completed(statePollerResult)) {
                syncPollingScheduler.schedule(task);
            }
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
            BooleanResult statePollerResult = ResourcesStatePollerResults.transformToFalseBooleanResult(authenticatedContext.getCloudContext());
            PollTask<BooleanResult> task = statusCheckFactory.newPollBooleanStateTask(authenticatedContext,
                    new ArmVirtualMachineStatusCheckerTask(armClient,
                            new VirtualMachineCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()),
                                    stackName, privateInstanceId, "Succeeded")));
            if (!task.completed(statePollerResult)) {
                syncPollingScheduler.schedule(task);
            }
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
