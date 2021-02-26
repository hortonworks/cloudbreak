package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.INSTANCE_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import rx.Completable;
import rx.schedulers.Schedulers;

@Component
public class AzureVirtualMachineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureVirtualMachineService.class);

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Map<String, VirtualMachine> getVirtualMachinesByName(AzureClient azureClient, String resourceGroup, Collection<String> privateInstanceIds) {
        LOGGER.debug("Starting to retrieve vm metadata from Azure for {} for ids: {}", resourceGroup, privateInstanceIds);
        PagedList<VirtualMachine> virtualMachines = azureClient.getVirtualMachines(resourceGroup);
        while (hasMissingVm(virtualMachines, privateInstanceIds) && virtualMachines.hasNextPage()) {
            virtualMachines.loadNextPage();
        }
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
        List<Completable> refreshInstanceViewCompletables = new ArrayList<>();
        for (VirtualMachine virtualMachine : virtualMachines.values()) {
            refreshInstanceViewCompletables.add(Completable.fromObservable(virtualMachine.refreshInstanceViewAsync()).subscribeOn(Schedulers.io()));
        }
        Completable.merge(refreshInstanceViewCompletables).await();
    }

    private boolean hasMissingVm(PagedList<VirtualMachine> virtualMachines, Collection<String> privateInstanceIds) {
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

    private Map<String, VirtualMachine> collectVirtualMachinesByName(Collection<String> privateInstanceIds, PagedList<VirtualMachine> virtualMachines) {
        return virtualMachines.stream()
                .filter(virtualMachine -> privateInstanceIds.contains(virtualMachine.name()))
                .collect(Collectors.toMap(HasName::name, vm -> vm));
    }

    private void validateResponse(PagedList<VirtualMachine> virtualMachines, Collection<String> privateInstanceIds) {
        if (hasMissingVm(virtualMachines, privateInstanceIds)) {
            LOGGER.warn("Failed to retrieve all host from Azure. Only {} found from the {}.", virtualMachines.size(), privateInstanceIds.size());
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
                virtualMachines.putAll(getVirtualMachinesByName(azureClient,
                        resourceGroupInstanceIdsMap.getKey(), resourceGroupInstanceIdsMap.getValue()));
            } catch (CloudException e) {
                LOGGER.debug("Exception occurred during the list of Virtual Machines by resource group", e);
                for (String instance : resourceGroupInstanceIdsMap.getValue()) {
                    cloudInstances.stream().filter(cloudInstance -> instance.equals(cloudInstance.getInstanceId())).findFirst().ifPresent(cloudInstance -> {
                        if (e.body() != null && "ResourceNotFound".equals(e.body().code())) {
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
}
