package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.ReactiveUtils;
import com.sequenceiq.cloudbreak.cloud.azure.util.SchedulerProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import reactor.core.publisher.Mono;

@Service
public class AzureInstanceConnector implements InstanceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceConnector.class);

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureVirtualMachineService azureVirtualMachineService;

    @Inject
    private SchedulerProvider schedulerProvider;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        throw new CloudOperationNotSupportedException("Azure ARM doesn't provide access to the VM console output yet.");
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        return startWithLimitedRetry(ac, resources, vms, null);
    }

    @Override
    public List<CloudVmInstanceStatus> startWithLimitedRetry(AuthenticatedContext ac, List<CloudResource> resources,
            List<CloudInstance> vms, Long timeboundInMs) {
        if (timeboundInMs == null) {
            LOGGER.info("Starting vms on Azure: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        } else {
            LOGGER.info("Starting vms on Azure: {} in {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()), timeboundInMs);
        }
        List<CloudVmInstanceStatus> statuses = new CopyOnWriteArrayList<>();
        List<Mono<Void>> startCompletables = new ArrayList<>();
        for (CloudInstance vm : vms) {
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), vm);
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            startCompletables.add(azureClient.startVirtualMachineAsync(resourceGroupName, vm.getInstanceId(), timeboundInMs)
                    .doOnError(throwable -> {
                        if (timeboundInMs != null) {
                            if (throwable instanceof TimeoutException) {
                                LOGGER.error("Timeout Error happened on azure instance start: {}", vm, throwable);
                                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.UNKNOWN, throwable.getMessage()));
                            } else {
                                LOGGER.error("Error happened on azure instance start: {}", vm, throwable);
                                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, throwable.getMessage()));
                            }
                        } else {
                            LOGGER.error("Error happend on azure instance start: {}", vm, throwable);
                            statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, throwable.getMessage()));
                        }
                    })
                    .doOnSuccess((i) -> statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.STARTED)))
                    .subscribeOn(schedulerProvider.io()));
        }
        ReactiveUtils.waitAll(startCompletables);
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        LOGGER.info("Stopping vms on Azure: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        return azureUtils.deallocateInstances(ac, vms);
    }

    @Override
    public List<CloudVmInstanceStatus> stopWithLimitedRetry(AuthenticatedContext ac, List<CloudResource> resources,
            List<CloudInstance> vms, Long timeboundInMs) {
        LOGGER.info("Stopping vms on Azure: {} in {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()), timeboundInMs);
        return azureUtils.deallocateInstancesWithLimitedRetry(ac, vms, timeboundInMs);
    }

    @Override
    public List<CloudVmInstanceStatus> reboot(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        LOGGER.info("Rebooting vms on Azure: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        List<CloudVmInstanceStatus> statuses = new CopyOnWriteArrayList<>();
        List<Mono<Void>> completables = new ArrayList<>();
        List<CloudVmInstanceStatus> currentStatuses = check(ac, vms);
        for (CloudVmInstanceStatus vm : currentStatuses) {
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(ac.getCloudContext(), vm.getCloudInstance());
            if (vm.getStatus() == InstanceStatus.STARTED) {
                completables.add(doReboot(vm, statuses, azureClient.stopVirtualMachineAsync(resourceGroupName, vm.getCloudInstance().getInstanceId())
                        .then(azureClient.startVirtualMachineAsync(resourceGroupName, vm.getCloudInstance().getInstanceId(), null))));
            } else if (vm.getStatus() == InstanceStatus.STOPPED) {
                completables.add(doReboot(vm, statuses, azureClient.startVirtualMachineAsync(resourceGroupName, vm.getCloudInstance().getInstanceId(), null)));
            } else {
                LOGGER.error(String.format("Unable to reboot instance %s because of invalid status %s.",
                        vm.getCloudInstance().getInstanceId(), vm.getStatus().toString()));
            }
        }
        ReactiveUtils.waitAll(completables);
        return statuses;
    }

    private Mono<Void> doReboot(CloudVmInstanceStatus vm, List<CloudVmInstanceStatus> statuses,
                                Mono<Void> asyncCall) {
        return asyncCall.doOnError(throwable -> {
                    LOGGER.error("Error happend on azure instance reboot: {}", vm, throwable);
                    statuses.add(new CloudVmInstanceStatus(vm.getCloudInstance(), InstanceStatus.FAILED, throwable.getMessage()));
                })
                .doOnSuccess((i) -> statuses.add(new CloudVmInstanceStatus(vm.getCloudInstance(), InstanceStatus.STARTED)))
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> cloudInstances) {
        LOGGER.info("Check instances on Azure: {}", cloudInstances.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        return azureVirtualMachineService.getVmsAndVmStatusesFromAzure(ac, cloudInstances).getStatuses();
    }

    @Override
    public List<CloudVmInstanceStatus> checkWithoutRetry(AuthenticatedContext ac, List<CloudInstance> cloudInstances) {
        LOGGER.info("Check instances on Azure: {}", cloudInstances.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        return azureVirtualMachineService.getVmsAndVmStatusesFromAzureWithoutRetry(ac, cloudInstances).getStatuses();
    }
}
