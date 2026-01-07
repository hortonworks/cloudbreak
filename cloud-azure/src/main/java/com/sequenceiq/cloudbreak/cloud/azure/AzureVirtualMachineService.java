package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.INSTANCE_NAME;
import static com.sequenceiq.common.model.DefaultApplicationTag.RESOURCE_CRN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.compute.models.PowerState;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineInstanceView;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasName;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.ReactiveUtils;
import com.sequenceiq.cloudbreak.cloud.azure.util.SchedulerProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import reactor.core.publisher.Mono;

@Component
public class AzureVirtualMachineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureVirtualMachineService.class);

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private SchedulerProvider schedulerProvider;

    @Inject
    private AzureExceptionHandler azureExceptionHandler;

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 60, noRetryFor = ProviderAuthenticationFailedException.class)
    public Map<String, VirtualMachine> getVirtualMachinesByName(AzureClient azureClient, String resourceGroup, Collection<String> privateInstanceIds) {
        LOGGER.debug("Starting to retrieve vm metadata from Azure for {} for ids: {}", resourceGroup, privateInstanceIds);
        List<VirtualMachine> virtualMachines = getVirtualMachinesByPrivateInstanceIds(azureClient, resourceGroup, privateInstanceIds);
        errorIfEmpty(virtualMachines);
        LOGGER.debug("Virtual machines from Azure: {}", virtualMachines.stream().map(HasName::name).collect(Collectors.joining(", ")));
        validateResponse(virtualMachines, privateInstanceIds);
        return collectVirtualMachinesByName(privateInstanceIds, virtualMachines);
    }

    private List<VirtualMachine> getVirtualMachinesByPrivateInstanceIds(
            AzureClient azureClient, String resourceGroup, Collection<String> privateInstanceIds) {
        List<VirtualMachine> virtualMachines = azureClient.getVirtualMachines(resourceGroup).getWhile(vms -> !hasMissingVm(vms, privateInstanceIds));
        if (virtualMachines != null && !virtualMachines.isEmpty()) {
            return virtualMachines;
        } else {
            LOGGER.info("We could not receive any VM in resource group. Let's try to fetch VMs by instance ids.");
            for (String privateInstanceId : privateInstanceIds) {
                VirtualMachine virtualMachineByResourceGroup = azureClient.getVirtualMachineByResourceGroup(resourceGroup, privateInstanceId);
                if (virtualMachineByResourceGroup == null) {
                    LOGGER.info("Could not find vm with private id: " + privateInstanceId);
                } else {
                    virtualMachines.add(virtualMachineByResourceGroup);
                }
            }
            return virtualMachines;
        }
    }

    private Map<String, VirtualMachine> getVirtualMachinesByNameEmptyAllowed(
            AzureClient azureClient, String resourceGroup, Collection<String> privateInstanceIds) {
        LOGGER.debug("Starting to retrieve vm metadata gracefully from Azure for {} for ids: {}", resourceGroup, privateInstanceIds);
        List<VirtualMachine> virtualMachines = getVirtualMachinesByPrivateInstanceIds(azureClient, resourceGroup, privateInstanceIds);
        validateResponse(virtualMachines, privateInstanceIds);
        return collectVirtualMachinesByName(privateInstanceIds, virtualMachines);
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public AzureVirtualMachinesWithStatuses getVmsAndVmStatusesFromAzure(AuthenticatedContext ac, List<CloudInstance> cloudInstances) {
        return getUpdatedVMs(ac, cloudInstances);
    }

    public AzureVirtualMachinesWithStatuses getVmsAndVmStatusesFromAzureWithoutRetry(AuthenticatedContext ac, List<CloudInstance> cloudInstances) {
        return getUpdatedVMs(ac, cloudInstances);
    }

    private AzureVirtualMachinesWithStatuses getUpdatedVMs(AuthenticatedContext ac, List<CloudInstance> cloudInstances) {
        AzureVirtualMachinesWithStatuses virtualMachinesWithStatuses = getVmsFromAzureAndFillStatusesIfResourceGroupRemoved(ac, cloudInstances);
        LOGGER.info("VirtualMachines from Azure: {}", virtualMachinesWithStatuses.getVirtualMachines().keySet());
        refreshInstanceViews(virtualMachinesWithStatuses.getVirtualMachines());
        fillVmStatuses(cloudInstances, virtualMachinesWithStatuses);
        return virtualMachinesWithStatuses;
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public void refreshInstanceViews(Map<String, VirtualMachine> virtualMachines) {
        LOGGER.info("Parallel instance views refresh to download instance view fields from azure, like PowerState of the machines: {}",
                virtualMachines.keySet());
        List<Mono<VirtualMachineInstanceView>> refreshInstanceViewCompletables = new ArrayList<>();
        for (VirtualMachine virtualMachine : virtualMachines.values()) {
            refreshInstanceViewCompletables.add(virtualMachine.refreshInstanceViewAsync().subscribeOn(schedulerProvider.io()));
        }
        ReactiveUtils.waitAll(refreshInstanceViewCompletables);
    }

    private boolean hasMissingVm(List<VirtualMachine> virtualMachines, Collection<String> privateInstanceIds) {
        Set<String> virtualMachineNames = virtualMachines
                .stream()
                .map(VirtualMachine::name)
                .collect(Collectors.toSet());
        boolean hasMissingVm = !virtualMachineNames.containsAll(privateInstanceIds);
        if (hasMissingVm) {
            LOGGER.info("Fetched VM id-s ({}) do not contain one of the following id-s: {}", virtualMachineNames, privateInstanceIds);
        }
        return hasMissingVm;
    }

    private Map<String, VirtualMachine> collectVirtualMachinesByName(Collection<String> privateInstanceIds, List<VirtualMachine> virtualMachines) {
        return virtualMachines.stream()
                .filter(virtualMachine -> privateInstanceIds.contains(virtualMachine.name()))
                .collect(Collectors.toMap(HasName::name, vm -> vm));
    }

    private void validateResponse(List<VirtualMachine> virtualMachines, Collection<String> privateInstanceIds) {
        if (hasMissingVm(virtualMachines, privateInstanceIds)) {
            LOGGER.warn("Failed to retrieve all host from Azure. Only {} found from the {}.", virtualMachines.size(), privateInstanceIds.size());
        }
    }

    private void errorIfEmpty(List<VirtualMachine> virtualMachines) {
        if (virtualMachines.isEmpty()) {
            LOGGER.warn("Azure returned 0 vms when listing by resource group. This should not be possible, retrying");
            throw new CloudConnectorException("Operation failed, azure returned an empty list while trying to list vms in resource group.");
        }
    }

    private AzureVirtualMachinesWithStatuses getVmsFromAzureAndFillStatusesIfResourceGroupRemoved(AuthenticatedContext ac, List<CloudInstance> cloudInstances) {
        LOGGER.info("Get vms from azure: {}", cloudInstances);
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        AzureClient azureClient = ac.getParameter(AzureClient.class);
        ArrayListMultimap<String, String> resourceGroupInstanceMultimap = cloudInstances.stream()
                .collect(Multimaps.toMultimap(
                        cloudInstance -> azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), cloudInstance),
                        CloudInstance::getInstanceId,
                        ArrayListMultimap::create));

        Map<String, VirtualMachine> virtualMachines = new HashMap<>();
        for (Map.Entry<String, Collection<String>> resourceGroupInstanceIdsMap : resourceGroupInstanceMultimap.asMap().entrySet()) {
            LOGGER.info("Get vms for resource group and add to all virtual machines: {}", resourceGroupInstanceIdsMap.getKey());
            try {
                virtualMachines.putAll(getVirtualMachinesByNameEmptyAllowed(azureClient,
                        resourceGroupInstanceIdsMap.getKey(), resourceGroupInstanceIdsMap.getValue()));
            } catch (ManagementException e) {
                LOGGER.debug("Exception occurred during the list of Virtual Machines by resource group", e);
                for (String instance : resourceGroupInstanceIdsMap.getValue()) {
                    cloudInstances.stream()
                            .filter(cloudInstance -> instance != null && instance.equals(cloudInstance.getInstanceId()))
                            .findFirst()
                            .ifPresent(cloudInstance -> {
                                if (azureExceptionHandler.isNotFound(e)) {
                                    statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.TERMINATED));
                                } else {
                                    String msg = String.format("Failed to get VM's state from Azure: %s", e.toString());
                                    statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.UNKNOWN, msg));
                                }
                            });
                }
            }
        }
        return new AzureVirtualMachinesWithStatuses(virtualMachines, statuses);
    }

    private void fillVmStatuses(List<CloudInstance> cloudInstances, AzureVirtualMachinesWithStatuses virtualMachineListResult) {
        Map<String, VirtualMachine> virtualMachines = virtualMachineListResult.getVirtualMachines();
        List<CloudVmInstanceStatus> statuses = virtualMachineListResult.getStatuses();
        LOGGER.info("Fill vm statuses from returned virtualmachines from azure: {}", virtualMachines.keySet());
        for (CloudInstance cloudInstance : cloudInstances) {
            virtualMachines.values().stream()
                    .filter(virtualMachine -> virtualMachine.name().equals(cloudInstance.getInstanceId()))
                    .findFirst()
                    .ifPresentOrElse(virtualMachine -> {
                        PowerState virtualMachinePowerState = virtualMachine.powerState();
                        String computerName = virtualMachine.computerName();
                        cloudInstance.putParameter(INSTANCE_NAME, computerName);
                        statuses.add(new CloudVmInstanceStatus(cloudInstance, AzureInstanceStatus.get(virtualMachinePowerState)));
                    }, () -> statuses.stream()
                            .filter(cvis -> cvis.getCloudInstance().getInstanceId() != null
                                    && cvis.getCloudInstance().getInstanceId().equals(cloudInstance.getInstanceId()))
                            .findAny()
                            .ifPresentOrElse(cloudInstanceWithStatus -> logTheStatusOfTheCloudInstance(cloudInstanceWithStatus),
                                    () -> statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.TERMINATED))));
        }
    }

    private void logTheStatusOfTheCloudInstance(CloudVmInstanceStatus cloudInstanceWithStatus) {
        LOGGER.info("Cloud instance '{}' could not be found in the response from Azure, but it's status already requested to be updated to '{}'",
                cloudInstanceWithStatus.getCloudInstance().getInstanceId(), cloudInstanceWithStatus.getStatus().name());
    }

    public List<InstanceCheckMetadata> collectCdpInstances(AuthenticatedContext ac, String resourceCrn, CloudStack cloudStack, List<String> knownInstanceIds) {
        LOGGER.info("Collecting CDP instances for stack with resource crn: '{}'", resourceCrn);
        AzureClient client = ac.getParameter(AzureClient.class);
        String resourceGroup = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), cloudStack);
        Map<String, VirtualMachine> vms = client.getVirtualMachines(resourceGroup).getStream()
                .filter(vm -> vm.tags().containsKey(RESOURCE_CRN.key()) && vm.tags().get(RESOURCE_CRN.key()).equals(resourceCrn))
                .collect(Collectors.toMap(VirtualMachine::name, Function.identity()));
        vms.putAll(retrieveKnownInstancesFromProviderIfAnyMissing(knownInstanceIds, vms, client, resourceGroup));

        LOGGER.info("Collected the following instances for stack with resource crn: '{}': {}", resourceCrn, vms.keySet());
        return vms.values().stream()
                .map(vm -> InstanceCheckMetadata.builder()
                        .withInstanceId(vm.name())
                        .withInstanceType(vm.size().getValue())
                        .withStatus(AzureInstanceStatus.get(vm.powerState()))
                        .build())
                .toList();
    }

    private Map<String, VirtualMachine> retrieveKnownInstancesFromProviderIfAnyMissing(List<String> knownInstanceIds,
            Map<String, VirtualMachine> instancesRetrievedByTag, AzureClient client, String resourceGroup) {
        Set<String> instanceIdsRetrievedByTag = instancesRetrievedByTag.keySet();
        List<String> knownInstanceIdsNotRetrievedByTag = knownInstanceIds.stream().filter(Predicate.not(instanceIdsRetrievedByTag::contains)).toList();
        if (!knownInstanceIdsNotRetrievedByTag.isEmpty()) {
            return getVirtualMachinesByPrivateInstanceIds(client, resourceGroup, knownInstanceIdsNotRetrievedByTag).stream()
                    .collect(Collectors.toMap(VirtualMachine::name, Function.identity()));
        }
        return Map.of();
    }
}
