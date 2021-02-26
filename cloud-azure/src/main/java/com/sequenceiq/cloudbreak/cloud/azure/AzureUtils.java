package com.sequenceiq.cloudbreak.cloud.azure;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentOperation;
import com.microsoft.azure.management.resources.DeploymentOperations;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureStatusMapper;
import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.azure.task.networkinterface.NetworkInterfaceDetachCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzurePremiumValidatorService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.ResourceType;

import rx.Completable;
import rx.Observable;
import rx.exceptions.CompositeException;
import rx.schedulers.Schedulers;

@Component
public class AzureUtils {
    public static final String RG_NAME = "resourceGroupName";

    public static final String NETWORK_ID = "networkId";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUtils.class);

    private static final String NO_PUBLIC_IP = "noPublicIp";

    private static final int HOST_GROUP_LENGTH = 3;

    private static final int NETWORKINTERFACE_DETACH_CHECKING_INTERVAL = 5000;

    private static final int NETWORKINTERFACE_DETACH_CHECKING_MAXATTEMPT = 5;

    @Value("${cb.max.azure.resource.name.length:}")
    private int maxResourceNameLength;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private AzurePremiumValidatorService azurePremiumValidatorService;

    @Inject
    private AzurePollTaskFactory azurePollTaskFactory;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private SyncPollingScheduler syncPollingScheduler;

    @Inject
    private AzureVirtualMachineService azureVirtualMachineService;

    public CloudResource getTemplateResource(Iterable<CloudResource> resourceList) {
        for (CloudResource resource : resourceList) {
            if (resource.getType() == ResourceType.ARM_TEMPLATE) {
                return resource;
            }
        }
        throw new CloudConnectorException(String.format("No resource found: %s", ResourceType.ARM_TEMPLATE));
    }

    public static String getGroupName(String group) {
        String shortened = WordUtils.initials(group.replaceAll("_", " ")).toLowerCase();
        return shortened.length() <= HOST_GROUP_LENGTH ? shortened : shortened.substring(0, HOST_GROUP_LENGTH);
    }

    public static void removeBlankSpace(StringBuilder sb) {
        int j = 0;
        for (int i = 0; i < sb.length(); i++) {
            if (!Character.isWhitespace(sb.charAt(i))) {
                sb.setCharAt(j++, sb.charAt(i));
            }
        }
        sb.delete(j, sb.length());
    }

    public String getLoadBalancerId(String stackName) {
        return String.format("%s%s", stackName, "lb");
    }

    public String getPrivateInstanceId(String stackName, String groupName, String privateId) {
        return String.format("%s%s%s", stackName, getGroupName(groupName), privateId);
    }

    public String getStackName(CloudContext cloudContext) {
        return generateResourceGroupNameByNameAndId(cloudContext.getName(), cloudContext.getId().toString());
    }

    public String generateResourceGroupNameByNameAndId(String name, String id) {
        return Splitter.fixedLength(maxResourceNameLength - id.length())
                .splitToList(name).get(0) + id;
    }

    public CloudResourceStatus getTemplateStatus(CloudResource resource, Deployment templateDeployment, AzureClient access, String stackName) {
        String status = templateDeployment.provisioningState();
        LOGGER.debug("Azure stack status of: {}  is: {}", resource.getName(), status);
        ResourceStatus resourceStatus = AzureStatusMapper.mapResourceStatus(status);
        CloudResourceStatus armResourceStatus = null;

        if (ResourceStatus.FAILED.equals(resourceStatus)) {
            LOGGER.debug("Cloud resource status: {}", resourceStatus);
            try {
                DeploymentOperations templateDeploymentOperations = access.getTemplateDeploymentOperations(stackName, stackName);
                for (DeploymentOperation deploymentOperation : templateDeploymentOperations.list()) {

                    if ("Failed".equals(deploymentOperation.provisioningState())) {
                        String statusMessage = (String) deploymentOperation.statusMessage();
                        armResourceStatus = new CloudResourceStatus(resource, AzureStatusMapper.mapResourceStatus(status), statusMessage);
                        break;
                    }
                }
            } catch (RuntimeException e) {
                armResourceStatus = new CloudResourceStatus(resource, AzureStatusMapper.mapResourceStatus(status), e.getMessage());
            }
        } else {
            LOGGER.debug("Cloud resource status: {}", resourceStatus);
            armResourceStatus = new CloudResourceStatus(resource, AzureStatusMapper.mapResourceStatus(status));
        }
        return armResourceStatus;
    }

    public boolean isExistingNetwork(Network network) {
        return isNotEmpty(getCustomNetworkId(network)) && isNotEmpty(getCustomResourceGroupName(network)) && isListNotEmpty(getCustomSubnetIds(network));
    }

    public String getInstanceName(CloudResource resource) {
        String instanceId = resource.getInstanceId();
        return Objects.nonNull(instanceId) ? StringUtils.substringAfterLast(instanceId, "/") : null;
    }

    private boolean isListNotEmpty(Collection<String> c) {
        return c != null && !c.isEmpty();
    }

    public boolean isPrivateIp(Network network) {
        return network.getParameters().containsKey(NO_PUBLIC_IP) ? network.getParameter(NO_PUBLIC_IP, Boolean.class) : false;
    }

    public List<CloudInstance> getInstanceList(CloudStack stack) {
        return stack.getGroups().stream().flatMap(group -> group.getInstances().stream()).collect(Collectors.toList());
    }

    public String getCustomNetworkId(Network network) {
        return network.getStringParameter(NETWORK_ID);
    }

    public String getCustomResourceGroupName(Network network) {
        return network.getStringParameter(RG_NAME);
    }

    public List<String> getCustomSubnetIds(Network network) {
        String subnetIds = network.getStringParameter(CloudInstance.SUBNET_ID);
        if (StringUtils.isBlank(subnetIds)) {
            return new ArrayList<>();
        }
        return Arrays.asList(subnetIds.split(","));
    }

    public void validateSubnet(AzureClient client, Network network) {
        if (isExistingNetwork(network)) {
            String resourceGroupName = getCustomResourceGroupName(network);
            String networkId = getCustomNetworkId(network);
            Collection<String> subnetIds = getCustomSubnetIds(network);
            for (String subnetId : subnetIds) {
                try {
                    Subnet subnet = client.getSubnetProperties(resourceGroupName, networkId, subnetId);
                    if (subnet == null) {
                        throw new CloudConnectorException(
                                String.format("Subnet [%s] is not found in resource group [%s] and network [%s]", subnetId, resourceGroupName, networkId)
                        );
                    }
                } catch (RuntimeException e) {
                    throw new CloudConnectorException("Subnet validation failed, cause: " + e.getMessage(), e);
                }
            }
        }
    }

    public void validateStorageType(CloudStack stack) {
        for (Group group : stack.getGroups()) {
            InstanceTemplate template = group.getReferenceInstanceTemplate();
            if (!template.getVolumes().isEmpty()) {
                String volumeType = template.getVolumes().get(0).getType();
                String flavor = template.getFlavor();
                AzureDiskType diskType = AzureDiskType.getByValue(volumeType);
                validateStorageTypeForGroup(diskType, flavor);
            } else {
                LOGGER.debug("No volume was attached for instance group {}, skipping storage validation", group.getName());
            }
        }
    }

    public void validateStorageTypeForGroup(AzureDiskType diskType, String flavor) {
        if (azurePremiumValidatorService.premiumDiskTypeConfigured(diskType)) {
            if (!azurePremiumValidatorService.validPremiumConfiguration(flavor)) {
                throw new CloudConnectorException("Only the DS instance types supports the premium storage.");
            }
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public List<CloudVmInstanceStatus> deallocateInstances(AuthenticatedContext ac, List<CloudInstance> vms) {
        LOGGER.info("Deallocate VMs: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        List<CloudVmInstanceStatus> actualVmStatuses = azureVirtualMachineService.getVmsAndVmStatusesFromAzureWithoutRetry(ac, vms).getStatuses();
        List<CloudVmInstanceStatus> statusesAfterDeallocate = new ArrayList<>();
        List<CloudInstance> vmsNotStopped = new ArrayList<>();
        actualVmStatuses.stream().forEach(instance -> {
            if (InstanceStatus.STOPPED.equals(instance.getStatus())) {
                statusesAfterDeallocate.add(instance);
            } else {
                vmsNotStopped.add(instance.getCloudInstance());
            }
        });
        LOGGER.info("Deallocate not stopped VMs: {}", vmsNotStopped.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        deallocateInstancesAndFillStatuses(ac, statusesAfterDeallocate, vmsNotStopped);

        return statusesAfterDeallocate;
    }

    private void deallocateInstancesAndFillStatuses(AuthenticatedContext ac, List<CloudVmInstanceStatus> statusesAfterDeallocate,
            List<CloudInstance> vmsNotStopped) {
        try {
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            List<Completable> deallocateCompletables = new ArrayList<>();
            for (CloudInstance vm : vmsNotStopped) {
                deallocateInstance(azureClient, ac.getCloudContext(), statusesAfterDeallocate, deallocateCompletables, vm);
            }
            Completable.mergeDelayError(deallocateCompletables).await();
        } catch (CompositeException e) {
            String errorMessages = e.getExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining());
            LOGGER.error("Error(s) occured while waiting for vm deallocation: {}, statuses: {}", errorMessages, statusesAfterDeallocate);
            throw new CloudbreakServiceException("Error(s) occured while waiting for vm deallocation: " + errorMessages
                    + ", statuses: " + convertToStatusText(statusesAfterDeallocate), e);
        } catch (RuntimeException e) {
            LOGGER.error("Error occured while waiting for vm deallocation: {}, statuses: {}", e.getMessage(), statusesAfterDeallocate, e);
            throw new CloudbreakServiceException("Error occured while waiting for vm deallocation: " + e.getMessage()
                    + ", statuses: " + convertToStatusText(statusesAfterDeallocate), e);
        }
    }

    private void deallocateInstance(AzureClient azureClient, CloudContext cloudContext, List<CloudVmInstanceStatus> statusesAfterDeallocate,
            List<Completable> deallocateCompletables, CloudInstance vm) {
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, vm);
        deallocateCompletables.add(azureClient.deallocateVirtualMachineAsync(resourceGroupName, vm.getInstanceId())
                .doOnError(throwable -> {
                    LOGGER.error("Error happend on azure instance stop: {}", vm, throwable);
                    statusesAfterDeallocate.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, throwable.getMessage()));
                })
                .doOnCompleted(() -> statusesAfterDeallocate.add(new CloudVmInstanceStatus(vm, InstanceStatus.STOPPED)))
                .subscribeOn(Schedulers.io()));
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public List<CloudVmInstanceStatus> deleteInstances(AuthenticatedContext ac, List<CloudInstance> vms) {
        LOGGER.info("Delete VMs: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        List<CloudVmInstanceStatus> statusesAfterDelete = new ArrayList<>();
        AzureVirtualMachinesWithStatuses virtualMachinesWithStatuses = azureVirtualMachineService.getVmsAndVmStatusesFromAzure(ac, vms);
        List<CloudInstance> vmsNotDeleted = new ArrayList<>();
        for (CloudVmInstanceStatus instance : virtualMachinesWithStatuses.getStatuses()) {
            if (virtualMachinesWithStatuses.getVirtualMachines().containsKey(instance.getCloudInstance().getInstanceId())) {
                if (InstanceStatus.TERMINATED.equals(instance.getStatus()) || InstanceStatus.TERMINATED_BY_PROVIDER.equals(instance.getStatus())) {
                    statusesAfterDelete.add(instance);
                } else {
                    vmsNotDeleted.add(instance.getCloudInstance());
                }
            }
        }
        LOGGER.info("Delete not stopped VMs: {}", vmsNotDeleted.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        deleteInstancesAndFillStatuses(ac, statusesAfterDelete, vmsNotDeleted);

        return statusesAfterDelete;
    }

    private void deleteInstancesAndFillStatuses(AuthenticatedContext ac, List<CloudVmInstanceStatus> statusesAfterDelete, List<CloudInstance> vmsNotDeleted) {
        try {
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            List<Completable> deleteCompletables = new ArrayList<>();
            for (CloudInstance vm : vmsNotDeleted) {
                deleteInstance(azureClient, ac.getCloudContext(), statusesAfterDelete, deleteCompletables, vm);
            }
            Completable.mergeDelayError(deleteCompletables).await();
        } catch (CompositeException e) {
            String errorMessages = e.getExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining());
            LOGGER.error("Error(s) occured while waiting for vm deletion: {}, statuses: {}", errorMessages, statusesAfterDelete);
            throw new CloudbreakServiceException("Error(s) occured while waiting for vm deletion: " + errorMessages
                    + ", statuses: " + convertToStatusText(statusesAfterDelete), e);
        } catch (RuntimeException e) {
            LOGGER.error("Error occured while waiting for vm deletion: {}, statuses: {}", e.getMessage(), statusesAfterDelete, e);
            throw new CloudbreakServiceException("Error occured while waiting for vm deletion: " + e.getMessage()
                    + ", statuses: " + convertToStatusText(statusesAfterDelete), e);
        }
    }

    private void deleteInstance(AzureClient azureClient, CloudContext cloudContext, List<CloudVmInstanceStatus> statusesAfterDelete,
            List<Completable> deleteCompletables, CloudInstance vm) {
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, vm);
        deleteCompletables.add(azureClient.deleteVirtualMachine(resourceGroupName, vm.getInstanceId())
                .doOnError(throwable -> {
                    LOGGER.error("Error happened on azure instance delete: {}", vm, throwable);
                    statusesAfterDelete.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, throwable.getMessage()));
                })
                .doOnCompleted(() -> statusesAfterDelete.add(new CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED)))
                .subscribeOn(Schedulers.io()));
    }

    private String convertToStatusText(List<CloudVmInstanceStatus> statusesAfterDeallocate) {
        return statusesAfterDeallocate.stream()
                .map(status -> status.getCloudInstance().getInstanceId() + " - " + status.getStatus())
                .collect(Collectors.joining());
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteInstancesByName(AuthenticatedContext ac, String resourceGroupName, List<String> instanceNameList) {
        LOGGER.info("Delete VM-s by name: {}", instanceNameList);
        List<String> failedToDeleteVms = new ArrayList<>();
        try {
            List<Completable> deleteCompletables = new ArrayList<>();
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            for (String vmName : instanceNameList) {
                deleteCompletables.add(azureClient.deleteVirtualMachine(resourceGroupName, vmName)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure instance delete: {}", vmName, throwable);
                            failedToDeleteVms.add(vmName);
                        })
                        .subscribeOn(Schedulers.io()));
            }
            Completable.mergeDelayError(deleteCompletables).await();
        } catch (CompositeException e) {
            String errorMessages = e.getExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining());
            LOGGER.error("Error(s) occured while waiting for vm deletion: {}", errorMessages);
            throw new CloudbreakServiceException("Error(s) occured while waiting for vm deletion: " + errorMessages, e);
        } catch (RuntimeException e) {
            LOGGER.error("Error occured while waiting for vm deletion: {}", e.getMessage(), e);
            throw new CloudbreakServiceException("Error occured while waiting for vm deletion: " + e.getMessage(), e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void waitForDetachNetworkInterfaces(AuthenticatedContext authenticatedContext, AzureClient azureClient, String resourceGroupName,
            Collection<String> networkInterfaceNames) {
        try {
            PollTask<Boolean> networkInterfaceDetachCheckerTask = azurePollTaskFactory.networkInterfaceDetachCheckerTask(
                    authenticatedContext, new NetworkInterfaceDetachCheckerContext(azureClient, resourceGroupName, networkInterfaceNames));
            syncPollingScheduler.schedule(networkInterfaceDetachCheckerTask, NETWORKINTERFACE_DETACH_CHECKING_INTERVAL,
                    NETWORKINTERFACE_DETACH_CHECKING_MAXATTEMPT, 1);
        } catch (Exception e) {
            throw new CloudConnectorException("Error during waiting for network detach: ", e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteNetworkInterfaces(AzureClient azureClient, String resourceGroupName, Collection<String> networkInterfaceNames) {
        LOGGER.info("Delete network interfaces: {}", networkInterfaceNames);
        List<String> failedToDeleteNetworkInterfaces = new ArrayList<>();
        try {
            List<Completable> deleteCompletables = new ArrayList<>();
            for (String networkInterfaceName : networkInterfaceNames) {
                deleteCompletables.add(azureClient.deleteNetworkInterfaceAsync(resourceGroupName, networkInterfaceName)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure network interface delete: {}", networkInterfaceName, throwable);
                            failedToDeleteNetworkInterfaces.add(networkInterfaceName);
                        })
                        .subscribeOn(Schedulers.io()));
            }
            Completable.mergeDelayError(deleteCompletables).await();
        } catch (CompositeException e) {
            String errorMessages = e.getExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining());
            LOGGER.error("Error(s) occured while waiting for network interface deletion: {}", errorMessages);
            throw new CloudbreakServiceException("Error(s) occured while waiting for network interface deletion: " + errorMessages, e);
        } catch (RuntimeException e) {
            LOGGER.error("Error occured while waiting for network interface deletion: {}", e.getMessage(), e);
            throw new CloudbreakServiceException("Error occured while waiting for network interface deletion: " + e.getMessage(), e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deletePublicIps(AzureClient azureClient, String resourceGroupName, Collection<String> publicIpNames) {
        LOGGER.info("Delete public ips: {}", publicIpNames);
        List<String> failedToDeletePublicIps = new ArrayList<>();
        try {
            List<Completable> deleteCompletables = new ArrayList<>();
            for (String publicIpName : publicIpNames) {
                deleteCompletables.add(azureClient.deletePublicIpAddressByNameAsync(resourceGroupName, publicIpName)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure public ip delete: {}", publicIpName, throwable);
                            failedToDeletePublicIps.add(publicIpName);
                        })
                        .subscribeOn(Schedulers.io()));
            }
            Completable.mergeDelayError(deleteCompletables).await();
        } catch (CompositeException e) {
            String errorMessages = e.getExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining());
            LOGGER.error("Error(s) occured while waiting for public ip deletion: {}", errorMessages);
            throw new CloudbreakServiceException("Error(s) occured while waiting for public ip deletion: " + errorMessages, e);
        } catch (RuntimeException e) {
            LOGGER.error("Error occured while waiting for public ip deletion: {}", e.getMessage(), e);
            throw new CloudbreakServiceException("Error occured while waiting for public ip deletion: " + e.getMessage(), e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteAvailabilitySets(AzureClient azureClient, String resourceGroupName, Collection<String> availabilitySetNames) {
        LOGGER.info("Delete availability sets: {}", availabilitySetNames);
        List<String> failedToDeleteAvailabiltySets = new ArrayList<>();
        try {
            List<Completable> deleteCompletables = new ArrayList<>();
            for (String availabilitySetName : availabilitySetNames) {
                deleteCompletables.add(azureClient.deleteAvailabilitySetAsync(resourceGroupName, availabilitySetName)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure availability set delete: {}", availabilitySetName, throwable);
                            failedToDeleteAvailabiltySets.add(availabilitySetName);
                        })
                        .subscribeOn(Schedulers.io()));
            }
            Completable.mergeDelayError(deleteCompletables).await();
        } catch (CompositeException e) {
            String errorMessages = e.getExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining());
            LOGGER.error("Error(s) occured while waiting for availability set deletion: {}", errorMessages);
            throw new CloudbreakServiceException("Error(s) occured while waiting for availability set deletion: " + errorMessages, e);
        } catch (RuntimeException e) {
            LOGGER.error("Error occured while waiting for availability set deletion: {}", e.getMessage(), e);
            throw new CloudbreakServiceException("Error occured while waiting for availability set deletion: " + e.getMessage(), e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteSecurityGroups(AzureClient azureClient, Collection<String> securityGroupIds) {
        if (CollectionUtils.isNotEmpty(securityGroupIds)) {
            LOGGER.info("Delete security groups with id-s: {}", securityGroupIds);

            Observable<String> deletionObservable = azureClient.deleteSecurityGroupsAsnyc(securityGroupIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the security groups ", throwable);
                        throw new CloudbreakServiceException("Can't delete all security groups: ", throwable);
                    })
                    .doOnCompleted(() -> LOGGER.debug("Delete security groups completed successfully"))
                    .subscribeOn(Schedulers.io());
            deletionObservable.subscribe(sg -> LOGGER.debug("Deleting {}", sg));
            deletionObservable.toCompletable().await();
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteNetworks(AzureClient azureClient, Collection<String> networkIds) {
        if (CollectionUtils.isNotEmpty(networkIds)) {
            LOGGER.info("Delete networks with id-s: {}", networkIds);

            Observable<String> deletionObservable = azureClient.deleteNetworksAsync(networkIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the networks ", throwable);
                        throw new CloudbreakServiceException("Can't delete all networks: ", throwable);
                    })
                    .doOnCompleted(() -> LOGGER.debug("Delete networks completed successfully"))
                    .subscribeOn(Schedulers.io());
            deletionObservable.subscribe(network -> LOGGER.debug("Deleting {}", network));
            deletionObservable.toCompletable().await();
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteStorageAccounts(AzureClient azureClient, Collection<String> accountIds) {
        if (CollectionUtils.isNotEmpty(accountIds)) {
            LOGGER.info("Delete storage accounts with id-s: {}", accountIds);

            Observable<String> deletionObservable = azureClient.deleteStorageAccountsAsync(accountIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the storage accounts ", throwable);
                        throw new CloudbreakServiceException("Can't delete all storage accounts: ", throwable);
                    })
                    .doOnCompleted(() -> LOGGER.debug("Delete storage accounts completed successfully"))
                    .subscribeOn(Schedulers.io());
            deletionObservable.subscribe(account -> LOGGER.debug("Deleting {}", account));
            deletionObservable.toCompletable().await();
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteImages(AzureClient azureClient, Collection<String> imageIds) {
        if (CollectionUtils.isNotEmpty(imageIds)) {
            LOGGER.info("Delete images with id-s: {}", imageIds);

            Observable<String> deletionObservable = azureClient.deleteImagesAsync(imageIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the images ", throwable);
                        throw new CloudbreakServiceException("Can't delete all images: ", throwable);
                    })
                    .doOnCompleted(() -> LOGGER.debug("Delete images completed successfully"))
                    .subscribeOn(Schedulers.io());
            deletionObservable.subscribe(image -> LOGGER.debug("Deleting {}", image));
            deletionObservable.toCompletable().await();
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteGenericResources(AzureClient azureClient, Collection<String> genericResourceIds) {
        LOGGER.info("Delete generic resources: {}", genericResourceIds);
        List<String> failedToDeleteGenericResources = new ArrayList<>();
        try {
            List<Completable> deleteCompletables = new ArrayList<>();
            for (String resourceId : genericResourceIds) {
                deleteCompletables.add(azureClient.deleteGenericResourceByIdAsync(resourceId)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure during generic delete: {}", resourceId, throwable);
                            failedToDeleteGenericResources.add(resourceId);
                        })
                        .subscribeOn(Schedulers.io()));
            }
            Completable.mergeDelayError(deleteCompletables).await();
        } catch (CompositeException e) {
            String errorMessages = e.getExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining());
            LOGGER.error("Error(s) occured while waiting for generic resource deletion: {}", errorMessages);
            throw new CloudbreakServiceException("Error(s) occured while waiting for generic resource deletion: " + errorMessages, e);
        } catch (RuntimeException e) {
            LOGGER.error("Error occured while waiting for generic resource deletion: {}", e.getMessage(), e);
            throw new CloudbreakServiceException("Error occured while waiting for generic resource deletion: " + e.getMessage(), e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Optional<String> deleteDatabaseServer(AzureClient azureClient, String databaseServerId, boolean cancelException) {
        return handleDeleteErrors(azureClient::deleteGenericResourceById, "DatabaseServer", databaseServerId, cancelException);
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Optional<String> deleteResourceGroup(AzureClient azureClient, String resourceGroupId, boolean cancelException) {
        return handleDeleteErrors(azureClient::deleteResourceGroup, "ResourceGroup", resourceGroupId, cancelException);
    }

    private <T> Optional<String> handleDeleteErrors(Consumer<String> deleteConsumer, String resourceType, String resourceId, boolean cancelException) {
        try {
            LOGGER.debug("Deleteing {} {}", resourceType, resourceId);
            deleteConsumer.accept(resourceId);
            return Optional.empty();
        } catch (CloudException e) {
            LOGGER.warn("Exception during resource delete", e);
            Optional<String> errorMessageOptional = getErrorMessage(resourceType, resourceId, e);
            if (errorMessageOptional.isEmpty()) {
                return Optional.empty();
            }
            if (cancelException) {
                LOGGER.warn(errorMessageOptional.get());
                LOGGER.warn("{} {} deletion failed, continuing because termination is forced", resourceType, resourceId);
                return errorMessageOptional;
            } else {
                LOGGER.warn(errorMessageOptional.get());
                throw new CloudConnectorException(errorMessageOptional.get(), e);
            }
        }
    }

    private Optional<String> getErrorMessage(String resourceType, String resourceId, CloudException e) {
        CloudError cloudError = e.body();
        if (cloudError == null) {
            return Optional.of(String.format("%s %s deletion failed: '%s', please go to Azure Portal for details",
                    resourceType, resourceId, e.getMessage()));
        }

        String errorCode = cloudError.code();
        if ("ResourceGroupNotFound".equals(errorCode)) {
            LOGGER.warn("{} {} does not exist, assuming that it has already been deleted", resourceType, resourceId);
            return Optional.empty();
            // leave errorMessage null => do not throw exception
        } else {
            String details = cloudError.details() != null ? cloudError.details().stream().map(CloudError::message).collect(Collectors.joining(", ")) : "";
            return Optional.of(String.format("%s %s deletion failed, status code %s, error message: %s, details: %s",
                    resourceType, resourceId, errorCode, cloudError.message(), details));
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteManagedDisks(AzureClient azureClient, Collection<String> managedDiskIds) {
        if (CollectionUtils.isNotEmpty(managedDiskIds)) {
            LOGGER.info("Delete managed disks with id-s: {}", managedDiskIds);

            Observable<String> deletionObservable = azureClient.deleteManagedDisksAsync(managedDiskIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the managed disks: " + managedDiskIds, throwable);
                        throw new CloudbreakServiceException("Can't delete all managed disks: " + managedDiskIds, throwable);
                    })
                    .doOnCompleted(() -> LOGGER.info("Delete managed disks completed successfully: {}", managedDiskIds))
                    .subscribeOn(Schedulers.io());
            deletionObservable.subscribe(disk -> LOGGER.info("Deleting {}, managed disks ids: {}", disk, managedDiskIds));
            deletionObservable.toCompletable().await();
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteManagedDisks(AzureClient azureClient, String resourceGroupName, Collection<String> managedDiskNames) {
        LOGGER.info("Deleting managed disks: {}", managedDiskNames);
        List<Completable> deleteCompletables = new ArrayList<>();
        List<String> failedToDeleteManagedDisks = new ArrayList<>();
        try {
            for (String resourceId : managedDiskNames) {
                deleteCompletables.add(azureClient.deleteManagedDiskAsync(resourceGroupName, resourceId)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure during managed disk deletion: {}", resourceId, throwable);
                            failedToDeleteManagedDisks.add(resourceId);
                        })
                        .subscribeOn(Schedulers.io()));
            }
            Completable.mergeDelayError(deleteCompletables).await();
        } catch (CompositeException e) {
            LOGGER.error("Error(s) occured while waiting for managed disks deletion: [{}]",
                    e.getExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining()));
            throw new CloudbreakServiceException("Can't delete every managed disk: " + failedToDeleteManagedDisks);
        } catch (RuntimeException e) {
            LOGGER.error("Error occured while waiting for managed disks deletion: {}", e.getMessage(), e);
            throw new CloudbreakServiceException("Can't delete every managed disk: " + failedToDeleteManagedDisks);
        }
    }

    public void checkResourceGroupExistence(AzureClient client, String resourceGroupName) {
        retryService.testWith2SecDelayMax5Times(() -> {
            if (!client.resourceGroupExists(resourceGroupName)) {
                throw new Retry.ActionFailedException("Resource group not exists");
            }
            return true;
        });
    }

    public CloudConnectorException convertToCloudConnectorException(CloudException e, String actionDescription) {
        LOGGER.warn("{} failed, cloud exception happened: ", actionDescription, e);
        if (e.body() != null && e.body().details() != null) {
            String details = e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", "));
            return new CloudConnectorException(String.format("%s failed, status code %s, error message: %s, details: %s",
                    actionDescription, e.body().code(), e.body().message(), details));
        } else {
            return new CloudConnectorException(String.format("%s failed: '%s', please go to Azure Portal for detailed message", actionDescription, e));
        }
    }

}
