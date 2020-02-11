package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import rx.Completable;
import rx.schedulers.Schedulers;

@Service
public class AzureInstanceConnector implements InstanceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceConnector.class);

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureVirtualMachineService azureVirtualMachineService;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        throw new CloudOperationNotSupportedException("Azure ARM doesn't provide access to the VM console output yet.");
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        LOGGER.info("Starting vms on Azure: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        List<Completable> startCompletables = new ArrayList<>();
        for (CloudInstance vm : vms) {
            String resourceGroupName = azureUtils.getResourceGroupName(ac.getCloudContext(), vm);
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            startCompletables.add(azureClient.startVirtualMachineAsync(resourceGroupName, vm.getInstanceId())
                    .doOnError(throwable -> {
                        LOGGER.error("Error happend on azure instance start: {}", vm, throwable);
                        statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, throwable.getMessage()));
                    })
                    .doOnCompleted(() -> statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.STARTED)))
                    .subscribeOn(Schedulers.io()));
        }
        Completable.merge(startCompletables).await();
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        LOGGER.info("Stopping vms on Azure: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        return azureUtils.deallocateInstances(ac, vms);
    }

    @Override
    public List<CloudVmInstanceStatus> reboot(AuthenticatedContext ac, List<CloudInstance> vms) {
        LOGGER.info("Rebooting vms on Azure: {}", vms.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        List<Completable> rebootCompletables = new ArrayList<>();
        for (CloudInstance vm : vms) {
            String resourceGroupName = azureUtils.getResourceGroupName(ac.getCloudContext(), vm);
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            rebootCompletables.add(azureClient.rebootVirtualMachineAsync(resourceGroupName, vm.getInstanceId())
                    .doOnError(throwable -> {
                        LOGGER.error("Error happend on azure instance reboot: {}", vm, throwable);
                        statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, throwable.getMessage()));
                    })
                    .doOnCompleted(() -> statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.STARTED)))
                    .subscribeOn(Schedulers.io()));
        }
        Completable.merge(rebootCompletables).await();
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> cloudInstances) {
        LOGGER.info("Check instances on Azure: {}", cloudInstances.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList()));
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        azureVirtualMachineService.getVmsFromAzureAndFillStatuses(ac, cloudInstances, statuses);
        return statuses;
    }
}
