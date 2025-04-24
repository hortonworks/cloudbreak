package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.DATABASE_PRIVATE_DNS_ZONE_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NETWORK_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NO_PUBLIC_IP;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.RESOURCE_GROUP_NAME;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import jakarta.inject.Inject;

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
import org.springframework.util.DigestUtils;

import com.azure.core.management.exception.AdditionalInfo;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.compute.models.PolicyViolation;
import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerSkuType;
import com.azure.resourcemanager.network.models.Subnet;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentOperation;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.resourcemanager.storage.models.Kind;
import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureStatusMapper;
import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.azure.task.networkinterface.NetworkInterfaceDetachCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.ReactiveUtils;
import com.sequenceiq.cloudbreak.cloud.azure.util.SchedulerProvider;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzurePremiumValidatorService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudExceptionConverter;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
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
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.ResourceType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class AzureUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUtils.class);

    private static final int HOST_GROUP_LENGTH = 3;

    private static final int NETWORKINTERFACE_DETACH_CHECKING_INTERVAL = 20000;

    private static final int NETWORKINTERFACE_DETACH_CHECKING_MAXATTEMPT = 10;

    private static final int MAX_DISK_ENCRYPTION_SET_NAME_LENGTH = 80;

    private static final int INSTANCE_NAME_HASH_LENGTH = 8;

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

    @Inject
    private CloudExceptionConverter cloudExceptionConverter;

    @Inject
    private SchedulerProvider schedulerProvider;

    @Inject
    private AzureExceptionHandler azureExceptionHandler;

    public static String getGroupName(String group) {
        String shortened = WordUtils.initials(group.replaceAll("_", " ")).toLowerCase(Locale.ROOT);
        return shortened.length() <= HOST_GROUP_LENGTH ? shortened : shortened.substring(0, HOST_GROUP_LENGTH);
    }

    public String getResourceName(String resourceId) {
        return StringUtils.substringAfterLast(resourceId, "/");
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

    public static String getInstanceIdWithoutStackName(String groupName, String privateId, String dbId) {
        if (dbId != null) {
            return String.format("%s%s-%s", getGroupName(groupName), privateId,
                    DigestUtils.md5DigestAsHex(dbId.getBytes()).substring(0, INSTANCE_NAME_HASH_LENGTH));
        } else {
            return String.format("%s%s", getGroupName(groupName), privateId);
        }
    }

    public CloudResource getTemplateResource(Iterable<CloudResource> resourceList) {
        for (CloudResource resource : resourceList) {
            if (resource.getType() == ResourceType.ARM_TEMPLATE) {
                return resource;
            }
        }
        throw new CloudConnectorException(String.format("No resource found: %s", ResourceType.ARM_TEMPLATE));
    }

    public String getFullInstanceId(String stackName, String groupName, String privateId, String dbId) {
        return stackName + getInstanceIdWithoutStackName(groupName, privateId, dbId);
    }

    public String getStackName(CloudContext cloudContext) {
        return generateResourceNameByNameAndId(cloudContext.getName(), cloudContext.getId().toString());
    }

    public String generateResourceNameByNameAndId(String name, String id) {
        return Splitter.fixedLength(maxResourceNameLength - id.length())
                .splitToList(name).getFirst() + id;
    }

    public String generateDesNameByNameAndId(String name, String id) {
        return Splitter.fixedLength(MAX_DISK_ENCRYPTION_SET_NAME_LENGTH - id.length())
                .splitToList(name).getFirst() + id;
    }

    public CloudResourceStatus getTemplateStatus(CloudResource resource, Deployment templateDeployment, AzureClient client, String stackName) {
        String status = templateDeployment.provisioningState();
        LOGGER.debug("Azure stack status of: {}  is: {}", resource.getName(), status);
        ResourceStatus resourceStatus = AzureStatusMapper.mapResourceStatus(status);
        CloudResourceStatus armResourceStatus = null;

        if (ResourceStatus.FAILED.equals(resourceStatus)) {
            LOGGER.debug("Cloud resource status: {}", resourceStatus);
            try {
                List<DeploymentOperation> templateDeploymentOperations = client.getTemplateDeploymentOperations(stackName, stackName).getAll();
                for (DeploymentOperation deploymentOperation : templateDeploymentOperations) {

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

    public Set<Kind> getSupportedAzureStorageKinds() {
        return Set.of(Kind.STORAGE_V2, Kind.BLOCK_BLOB_STORAGE);
    }

    public boolean isExistingNetwork(Network network) {
        return isNotEmpty(getCustomNetworkId(network)) && isNotEmpty(getCustomResourceGroupName(network))
                && CollectionUtils.isNotEmpty(getCustomSubnetIds(network));
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

    public String getPrivateDnsZoneId(Network network) {
        return network.getStringParameter(DATABASE_PRIVATE_DNS_ZONE_ID);
    }

    public String getCustomResourceGroupName(Network network) {
        return network.getStringParameter(RESOURCE_GROUP_NAME);
    }

    public List<String> getCustomSubnetIds(Network network) {
        String subnetIds = network.getStringParameter(SUBNET_ID);
        if (StringUtils.isBlank(subnetIds)) {
            return new ArrayList<>();
        }
        return Arrays.asList(subnetIds.split(","));
    }

    public List<String> getCustomEndpointGatewaySubnetIds(Network network) {
        String subnetIds = network.getStringParameter(ENDPOINT_GATEWAY_SUBNET_ID);
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
        deallocateInstancesAndFillStatuses(ac, statusesAfterDeallocate, vmsNotStopped, null);

        return statusesAfterDeallocate;
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000), maxAttempts = 3)
    public List<CloudVmInstanceStatus> deallocateInstancesWithLimitedRetry(AuthenticatedContext ac, List<CloudInstance> vms, Long timeboundInMs) {
        LOGGER.info("Deallocate VMs: {} in {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()), timeboundInMs);
        List<CloudVmInstanceStatus> actualVmStatuses = azureVirtualMachineService.getVmsAndVmStatusesFromAzureWithoutRetry(ac, vms).getStatuses();
        List<CloudVmInstanceStatus> statusesAfterDeallocate = new ArrayList<>();
        List<CloudInstance> vmsNotStopped = new ArrayList<>();
        Set<InstanceStatus> completedStatuses = EnumSet.of(
                InstanceStatus.FAILED,
                InstanceStatus.TERMINATED,
                InstanceStatus.TERMINATED_BY_PROVIDER,
                InstanceStatus.DELETE_REQUESTED,
                InstanceStatus.ZOMBIE,
                InstanceStatus.STOPPED);
        actualVmStatuses.stream().forEach(instance -> {
            if (completedStatuses.stream().anyMatch(instanceStatus -> instanceStatus.equals(instance.getStatus()))) {
                statusesAfterDeallocate.add(instance);
            } else {
                vmsNotStopped.add(instance.getCloudInstance());
            }
        });
        LOGGER.info("Deallocate not stopped VMs: {}", vmsNotStopped.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        deallocateInstancesAndFillStatuses(ac, statusesAfterDeallocate, vmsNotStopped, timeboundInMs);

        return statusesAfterDeallocate;
    }

    private void deallocateInstancesAndFillStatuses(AuthenticatedContext ac, List<CloudVmInstanceStatus> statusesAfterDeallocate,
            List<CloudInstance> vmsNotStopped, Long timeboundInMs) {
        try {
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            List<Mono<Void>> deallocateCompletables = new ArrayList<>();
            for (CloudInstance vm : vmsNotStopped) {
                deallocateInstance(azureClient, ac.getCloudContext(), statusesAfterDeallocate, deallocateCompletables, vm, timeboundInMs);
            }
            ReactiveUtils.waitAll(deallocateCompletables);
        } catch (Exception e) {
            String errorMessages = ReactiveUtils.getErrorMessageOrConcatSuppressedMessages(e);
            LOGGER.error("Error(s) occured while waiting for vm deallocation: {}, statuses: {}", errorMessages, statusesAfterDeallocate, e);
            throw new CloudbreakServiceException("Error(s) occured while waiting for vm deallocation: " + errorMessages
                    + ", statuses: " + convertToStatusText(statusesAfterDeallocate), e);
        }
    }

    private void deallocateInstance(AzureClient azureClient, CloudContext cloudContext, List<CloudVmInstanceStatus> statusesAfterDeallocate,
            List<Mono<Void>> deallocateCompletables, CloudInstance vm, Long timeboundInMs) {
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, vm);
        deallocateCompletables.add(azureClient.deallocateVirtualMachineAsync(resourceGroupName, vm.getInstanceId(), timeboundInMs)
                .doOnError(throwable -> {
                    if (timeboundInMs != null) {
                        if (throwable instanceof TimeoutException) {
                            LOGGER.error("Timeout Error happened on azure instance stop: {}", vm, throwable);
                            statusesAfterDeallocate.add(new CloudVmInstanceStatus(vm, InstanceStatus.UNKNOWN, throwable.getMessage()));
                        } else {
                            LOGGER.error("Error happened on azure instance stop: {}", vm, throwable);
                            statusesAfterDeallocate.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, throwable.getMessage()));
                        }
                    } else {
                        LOGGER.error("Error happend on azure instance stop: {}", vm, throwable);
                        statusesAfterDeallocate.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, throwable.getMessage()));
                    }
                })
                .doFinally((t) -> statusesAfterDeallocate.add(new CloudVmInstanceStatus(vm, InstanceStatus.STOPPED)))
                .subscribeOn(schedulerProvider.io()));
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public List<CloudVmInstanceStatus> deleteInstances(AuthenticatedContext ac, List<CloudInstance> vms) {
        LOGGER.info("Delete VMs: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        List<CloudVmInstanceStatus> statusesAfterDelete = new CopyOnWriteArrayList<>();
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
            List<Mono<Void>> deleteCompletables = vmsNotDeleted
                    .stream()
                    .map(vm -> deleteInstance(azureClient, ac.getCloudContext(), statusesAfterDelete, vm))
                    .collect(Collectors.toList());
            ReactiveUtils.waitAll(deleteCompletables);
        } catch (Exception e) {
            String errorMessages = ReactiveUtils.getErrorMessageOrConcatSuppressedMessages(e);
            LOGGER.error("Error(s) occured while waiting for vm deletion: {}, statuses: {}", errorMessages, statusesAfterDelete, e);
            throw new CloudbreakServiceException("Error(s) occured while waiting for vm deletion: " + errorMessages
                    + ", statuses: " + convertToStatusText(statusesAfterDelete), e);
        }
    }

    private Mono<Void> deleteInstance(AzureClient azureClient, CloudContext cloudContext, List<CloudVmInstanceStatus> statusesAfterDelete,
            CloudInstance vm) {
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, vm);
        return azureClient.deleteVirtualMachine(resourceGroupName, vm.getInstanceId())
                .doOnError(throwable -> {
                    LOGGER.error("Error happened on azure instance delete: {}", vm, throwable);
                    statusesAfterDelete.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, throwable.getMessage()));
                })
                .doOnSuccess((v) -> statusesAfterDelete.add(new CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED)))
                .subscribeOn(schedulerProvider.io());
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
            List<Mono<Void>> deleteCompletables = new ArrayList<>();
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            for (String vmName : instanceNameList) {
                deleteCompletables.add(azureClient.deleteVirtualMachine(resourceGroupName, vmName)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure instance delete: {}", vmName, throwable);
                            failedToDeleteVms.add(vmName);
                        })
                        .subscribeOn(schedulerProvider.io()));
            }
            ReactiveUtils.waitAll(deleteCompletables);
        } catch (Exception e) {
            logAndWrapCompositeException(e, "Error(s) occured while waiting for vm deletion: {}",
                    "Error(s) occured while waiting for vm deletion: ");
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
            throw new CloudConnectorException("Error during waiting for network detach: " + e.getMessage(), e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 30, multiplier = 2, maxDelay = 120), maxAttempts = 5)
    public void deleteNetworkInterfaces(AzureClient azureClient, String resourceGroupName, Collection<String> networkInterfaceNames) {
        LOGGER.info("Delete network interfaces: {}", networkInterfaceNames);
        List<String> failedToDeleteNetworkInterfaces = new ArrayList<>();
        try {
            List<Mono<Void>> deleteCompletables = new ArrayList<>();
            for (String networkInterfaceName : networkInterfaceNames) {
                deleteCompletables.add(azureClient.deleteNetworkInterfaceAsync(resourceGroupName, networkInterfaceName)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure network interface delete: {}", networkInterfaceName, throwable);
                            failedToDeleteNetworkInterfaces.add(networkInterfaceName);
                        })
                        .subscribeOn(schedulerProvider.io()));
            }
            ReactiveUtils.waitAll(deleteCompletables);
        } catch (Exception e) {
            logAndWrapCompositeException(e, "Error(s) occured while waiting for network interface deletion: {}",
                    "Error(s) occured while waiting for network interface deletion: ");
        }
    }

    @Retryable(backoff = @Backoff(delay = 30, multiplier = 2, maxDelay = 120), maxAttempts = 5)
    public void deletePublicIps(AzureClient azureClient, String resourceGroupName, Collection<String> publicIpNames) {
        LOGGER.info("Delete public ips: {}", publicIpNames);
        List<String> failedToDeletePublicIps = new ArrayList<>();
        try {
            List<Mono<Void>> deleteCompletables = new ArrayList<>();
            for (String publicIpName : publicIpNames) {
                deleteCompletables.add(azureClient.deletePublicIpAddressByNameAsync(resourceGroupName, publicIpName)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure public ip delete: {}", publicIpName, throwable);
                            failedToDeletePublicIps.add(publicIpName);
                        })
                        .subscribeOn(schedulerProvider.io()));
            }
            ReactiveUtils.waitAll(deleteCompletables);
        } catch (Exception e) {
            logAndWrapCompositeException(e, "Error(s) occured while waiting for public ip deletion: {}",
                    "Error(s) occured while waiting for public ip deletion: ");
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteLoadBalancers(AzureClient azureClient, String resourceGroupName, Collection<String> loadBalancerNames) {
        LOGGER.info("Deleting load balancers: {}", loadBalancerNames);
        try {
            List<Mono<Void>> deleteCompletables = new ArrayList<>();
            for (String loadBalancerName : loadBalancerNames) {
                deleteCompletables.add(azureClient.deleteLoadBalancerAsync(resourceGroupName, loadBalancerName)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure load balancer delete: {}", loadBalancerName, throwable);
                        })
                        .subscribeOn(schedulerProvider.io()));
            }
            ReactiveUtils.waitAll(deleteCompletables);
        } catch (Exception e) {
            logAndWrapCompositeException(e, "Error(s) occurred while waiting for load balancer deletion: {}",
                    "Error(s) occurred while waiting for load balancer deletion: ");
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public List<CloudLoadBalancer> describeLoadBalancers(AzureClient azureClient, String resourceGroupName,
            Collection<CloudLoadBalancerMetadata> loadBalancers) {
        List<CloudLoadBalancer> cloudLoadBalancers = new ArrayList<>();
        for (CloudLoadBalancerMetadata loadBalancerMetadata : loadBalancers) {
            LOGGER.info("Describing load balancer: {}", loadBalancerMetadata.getName());
            LoadBalancer loadBalancer = azureClient.getLoadBalancer(resourceGroupName, loadBalancerMetadata.getName());
            if (loadBalancer != null) {
                LoadBalancerSku loadBalancerSku = LoadBalancerSkuType.STANDARD.equals(loadBalancer.sku()) ? LoadBalancerSku.STANDARD : LoadBalancerSku.BASIC;
                cloudLoadBalancers.add(new CloudLoadBalancer(loadBalancerMetadata.getType(), loadBalancerSku, false));
            }
        }
        return cloudLoadBalancers;
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteAvailabilitySets(AzureClient azureClient, String resourceGroupName, Collection<String> availabilitySetNames) {
        LOGGER.info("Delete availability sets: {}", availabilitySetNames);
        List<String> failedToDeleteAvailabiltySets = new ArrayList<>();
        try {
            List<Mono<Void>> deleteCompletables = new ArrayList<>();
            for (String availabilitySetName : availabilitySetNames) {
                deleteCompletables.add(azureClient.deleteAvailabilitySetAsync(resourceGroupName, availabilitySetName)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure availability set delete: {}", availabilitySetName, throwable);
                            failedToDeleteAvailabiltySets.add(availabilitySetName);
                        })
                        .subscribeOn(schedulerProvider.io()));
            }
            ReactiveUtils.waitAll(deleteCompletables);
        } catch (Exception e) {
            logAndWrapCompositeException(e, "Error(s) occured while waiting for availability set deletion: {}",
                    "Error(s) occured while waiting for availability set deletion: ");
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteSecurityGroups(AzureClient azureClient, Collection<String> securityGroupIds) {
        if (CollectionUtils.isNotEmpty(securityGroupIds)) {
            LOGGER.info("Delete security groups with id-s: {}", securityGroupIds);

            Flux<String> deletionObservable = azureClient.deleteSecurityGroupsAsync(securityGroupIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the security groups ", throwable);
                        throw new CloudbreakServiceException("Can't delete all security groups: ", throwable);
                    })
                    .doOnComplete(() -> LOGGER.debug("Delete security groups completed successfully"))
                    .subscribeOn(schedulerProvider.io());
            deletionObservable.subscribe(sg -> LOGGER.debug("Deleting {}", sg));
            deletionObservable.blockLast();
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteNetworks(AzureClient azureClient, Collection<String> networkIds) {
        if (CollectionUtils.isNotEmpty(networkIds)) {
            LOGGER.info("Delete networks with id-s: {}", networkIds);

            Flux<String> deletionObservable = azureClient.deleteNetworksAsync(networkIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the networks ", throwable);
                        throw new CloudbreakServiceException("Can't delete all networks: ", throwable);
                    })
                    .doOnComplete(() -> LOGGER.debug("Delete networks completed successfully"))
                    .subscribeOn(schedulerProvider.io());
            deletionObservable.subscribe(network -> LOGGER.debug("Deleting {}", network));
            deletionObservable.blockLast();
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteStorageAccounts(AzureClient azureClient, Collection<String> accountIds) {
        if (CollectionUtils.isNotEmpty(accountIds)) {
            LOGGER.info("Delete storage accounts with id-s: {}", accountIds);

            Flux<String> deletionObservable = azureClient.deleteStorageAccountsAsync(accountIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the storage accounts ", throwable);
                        throw new CloudbreakServiceException("Can't delete all storage accounts: ", throwable);
                    })
                    .doOnComplete(() -> LOGGER.debug("Delete storage accounts completed successfully"))
                    .subscribeOn(schedulerProvider.io());
            deletionObservable.subscribe(account -> LOGGER.debug("Deleting {}", account));
            deletionObservable.blockLast();
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteImages(AzureClient azureClient, Collection<String> imageIds) {
        if (CollectionUtils.isNotEmpty(imageIds)) {
            LOGGER.info("Delete images with id-s: {}", imageIds);

            Flux<String> deletionObservable = azureClient.deleteImagesAsync(imageIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the images ", throwable);
                        throw new CloudbreakServiceException("Can't delete all images: ", throwable);
                    })
                    .doOnComplete(() -> LOGGER.debug("Delete images completed successfully"))
                    .subscribeOn(schedulerProvider.io());
            deletionObservable.subscribe(image -> LOGGER.debug("Deleting {}", image));
            deletionObservable.blockLast();
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteGenericResources(AzureClient azureClient, Collection<String> genericResourceIds) {
        LOGGER.info("Delete generic resources: {}", genericResourceIds);
        List<String> failedToDeleteGenericResources = new CopyOnWriteArrayList<>();
        try {
            List<Mono<Void>> deleteCompletables = new ArrayList<>();
            for (String resourceId : genericResourceIds) {
                deleteCompletables.add(azureClient.deleteGenericResourceByIdAsync(resourceId)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure during generic delete: {}", resourceId, throwable);
                            failedToDeleteGenericResources.add(resourceId);
                        })
                        .subscribeOn(schedulerProvider.io()));
            }
            ReactiveUtils.waitAll(deleteCompletables);
        } catch (Exception e) {
            logAndWrapCompositeException(e, "Error(s) occurred while waiting for generic resource deletion: {}",
                    "Error(s) occurred while waiting for generic resource deletion: ");
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Optional<String> deleteDatabaseServer(AzureClient azureClient, String databaseServerId, boolean cancelException) {
        String resourceType = "DatabaseServer";
        Optional<String> deleteErrors = handleDeleteErrors(azureClient::deleteGenericResourceById, resourceType, databaseServerId, cancelException);
        checkResourceIsDeleted(azureClient, databaseServerId, resourceType);
        return deleteErrors;
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Optional<String> deleteGenericResourceById(AzureClient azureClient, String id, AzureResourceType resourceType) {
        Optional<String> deleteErrors = handleDeleteErrors(azureClient::deleteGenericResourceById, resourceType.getAzureType(), id, false);
        checkResourceIsDeleted(azureClient, id, resourceType.getAzureType());
        return deleteErrors;
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Optional<String> deleteResourceGroup(AzureClient azureClient, String resourceGroupId, boolean cancelException) {
        return handleDeleteErrors(azureClient::deleteResourceGroup, "ResourceGroup", resourceGroupId, cancelException);
    }

    private void checkResourceIsDeleted(AzureClient azureClient, String resourceId, String resourceType) {
        GenericResource resource = azureClient.getGenericResourceById(resourceId);
        if (resource != null) {
            LOGGER.error("{} {} resource is still present after delete operation.", resourceType, resourceId);
        }
    }

    private Optional<String> handleDeleteErrors(Consumer<String> deleteConsumer, String resourceType, String resourceId, boolean cancelException) {
        try {
            LOGGER.debug("Deleting {} {}", resourceType, resourceId);
            deleteConsumer.accept(resourceId);
            return Optional.empty();
        } catch (ManagementException e) {
            LOGGER.warn("Exception during resource delete", e);
            Optional<String> errorMessageOptional = getDeletionErrorMessage(resourceType, resourceId, e);
            if (errorMessageOptional.isEmpty()) {
                return Optional.empty();
            }
            if (cancelException) {
                LOGGER.warn("{} {} deletion failed, continuing because termination is forced, details: {}",
                        resourceType, resourceId, errorMessageOptional.get());
                return errorMessageOptional;
            } else {
                LOGGER.warn("{} {} deletion failed, details: {}", resourceType, resourceId, errorMessageOptional.get());
                throw new CloudConnectorException(errorMessageOptional.get(), e);
            }
        }
    }

    private Optional<String> getDeletionErrorMessage(String resourceType, String resourceId, ManagementException e) {
        if (e.getValue() != null && azureExceptionHandler.isNotFound(e)) {
            LOGGER.warn("{} {} does not exist, assuming that it has already been deleted", resourceType, resourceId);
            return Optional.empty();
            // leave errorMessage null => do not throw exception
        } else {
            String errorMessage = getErrorMessage(e, resourceType + " deletion");
            return Optional.of(errorMessage);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteManagedDisks(AzureClient azureClient, Collection<String> managedDiskIds) {
        if (CollectionUtils.isNotEmpty(managedDiskIds)) {
            LOGGER.info("Delete managed disks with id-s: {}", managedDiskIds);

            Flux<String> deletionObservable = azureClient.deleteManagedDisksAsync(managedDiskIds)
                    .doOnError(throwable -> {
                        LOGGER.error("Error happened during the deletion of the managed disks: {}", managedDiskIds, throwable);
                        throw new CloudbreakServiceException("Can't delete all managed disks: " + managedDiskIds, throwable);
                    })
                    .doOnComplete(() -> LOGGER.info("Delete managed disks completed successfully: {}", managedDiskIds))
                    .subscribeOn(schedulerProvider.io());
            deletionObservable.subscribe(disk -> LOGGER.info("Deleting {}, managed disks ids: {}", disk, managedDiskIds));
            deletionObservable.blockLast();
        }
    }

    /**
     * The functionality offered by Azure does not work. Marketplace image users should accept the terms and conditions manually.
     *
     * @param azureClient           AzureClient
     * @param azureMarketplaceImage The image to be signed
     */
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void signImageConsent(AzureClient azureClient, AzureMarketplaceImage azureMarketplaceImage) {
        LOGGER.info("Signing terms and conditions for azure marketplace image: {}", azureMarketplaceImage);
        try {
            azureClient.signImageConsent(azureMarketplaceImage);
            LOGGER.info("Successfully signed terms and conditions of azure marketplace image: {}", azureMarketplaceImage);
        } catch (Exception e) {
            LOGGER.error("Error happened during signing the image consent: " + azureMarketplaceImage, e);
            throw new CloudbreakServiceException("Cannot sign terms and conditions of azure marketplace image: " + azureMarketplaceImage, e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void deleteManagedDisks(AzureClient azureClient, String resourceGroupName, Collection<String> managedDiskNames) {
        LOGGER.info("Deleting managed disks: {}", managedDiskNames);
        List<Mono<Void>> deleteCompletables = new ArrayList<>();
        List<String> failedToDeleteManagedDisks = new CopyOnWriteArrayList<>();
        try {
            for (String resourceId : managedDiskNames) {
                deleteCompletables.add(azureClient.deleteManagedDiskAsync(resourceGroupName, resourceId)
                        .doOnError(throwable -> {
                            LOGGER.error("Error happened on azure during managed disk deletion: {}", resourceId, throwable);
                            failedToDeleteManagedDisks.add(resourceId);
                        })
                        .subscribeOn(schedulerProvider.io()));
            }
            ReactiveUtils.waitAll(deleteCompletables);
        } catch (Exception e) {
            LOGGER.error("Error(s) occured while waiting for managed disks deletion: [{}]", e.getMessage());
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

    public boolean checkResourceGroupExistenceWithRetry(AzureClient client, String resourceGroupName) {
        return retryService.testWith2SecDelayMax5Times(() -> {
            try {
                return client.resourceGroupExists(resourceGroupName);
            } catch (Exception ex) {
                throw new Retry.ActionFailedException(ex);
            }
        });
    }

    public CloudConnectorException convertToCloudConnectorException(ManagementException e, String actionDescription) {
        String errorMessage = getErrorMessage(e, actionDescription);
        return convertToCloudConnectorException(e, actionDescription, errorMessage);
    }

    public CloudConnectorException convertToCloudConnectorExceptionWithFailureReason(ManagementException e, String actionDescription,
            String operationFailureReason) {
        String errorMessage = operationFailureReason == null
                ? getErrorMessage(e, actionDescription)
                : String.format("%s, Deployment operation failure reason: %s", getErrorMessage(e, actionDescription), operationFailureReason);
        return convertToCloudConnectorException(e, actionDescription, errorMessage);
    }

    private CloudConnectorException convertToCloudConnectorException(ManagementException e, String actionDescription, String errorMessage) {
        LOGGER.warn("{} failed, cloud exception happened, details: {}", actionDescription, errorMessage, e);
        if (e.getValue() != null && marketplaceRelatedError(e.getValue().getCode(), e.getValue().getMessage())) {
            return new CloudImageException(errorMessage);
        } else {
            return new CloudConnectorException(errorMessage);
        }
    }

    private String getErrorMessage(ManagementException e, String actionDescription) {
        if (e.getValue() != null && e.getValue().getDetails() != null) {
            String details = e.getValue().getDetails().stream().map(this::getCloudErrorMessage).collect(Collectors.joining(", "));
            return String.format("%s failed, status code %s, error message: %s, details: %s",
                    actionDescription, e.getValue().getCode(), e.getValue().getMessage(), details);
        } else {
            return String.format("%s failed: '%s', please go to Azure Portal for detailed message", actionDescription, e);
        }
    }

    private boolean marketplaceRelatedError(String errorCode, String errorMessage) {
        return Arrays.stream(AzureDeploymentMarketplaceError.values())
                .filter(error -> error.getCode().equalsIgnoreCase(errorCode))
                .anyMatch(error -> errorMessage.contains(error.getMessageFragment()));
    }

    private String getCloudErrorMessage(ManagementError cloudError) {
        if (cloudError.getCode().equals("RequestDisallowedByPolicy")) {
            String message = cloudError.getMessage().replace("See error details for policy resource IDs.", "");
            return message + cloudError.getAdditionalInfo()
                    .stream()
                    .map(AdditionalInfo::getInfo)
                    .filter(PolicyViolation.class::isInstance)
                    .map(PolicyViolation.class::cast)
                    .map(policyViolation -> "Policy definition: " + policyViolation.category() + " - " + policyViolation.details())
                    .collect(Collectors.joining(". "));
        } else if (cloudError.getDetails() != null) {
            String details = cloudError.getDetails().stream()
                    .filter(detail -> detail != null)
                    .map(ManagementError::getMessage)
                    .collect(Collectors.joining(", "));
            return String.format("%s (details: %s)", cloudError.getMessage(), details);
        } else {
            return cloudError.getMessage();
        }
    }

    public CloudConnectorException convertToCloudConnectorException(Throwable e, String actionDescription) {
        CloudConnectorException result;
        if (e instanceof ManagementException) {
            result = convertToCloudConnectorException((ManagementException) e, actionDescription);
        } else {
            result = cloudExceptionConverter.convertToCloudConnectorException(e, actionDescription);
        }
        return result;
    }

    public Retry.ActionFailedException convertToActionFailedExceptionCausedByCloudConnectorException(Throwable e, String actionDescription) {
        return Retry.ActionFailedException.ofCause(convertToCloudConnectorException(e, actionDescription));
    }

    // Encode input to 8 digit hex output
    public String encodeString(String string) {
        if (StringUtils.isNotBlank(string)) {
            byte[] bytes = string.getBytes();
            Checksum checksum = new Adler32();
            checksum.reset();
            checksum.update(bytes, 0, bytes.length);
            long checksumValue = checksum.getValue();
            return Long.toHexString(checksumValue).toLowerCase(Locale.ROOT);
        } else {
            return "";
        }
    }

    private void logAndWrapCompositeException(Exception ce, String formatString, String exceptionString) {
        String errorMessages = ce.getMessage();
        LOGGER.error(formatString, errorMessages);
        throw new CloudbreakServiceException(exceptionString + errorMessages, ce);
    }
}