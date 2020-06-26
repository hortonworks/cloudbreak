package com.sequenceiq.it.cloudbreak.util.azure.azurevm.action;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasId;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.azure.AzureInstanceActionExecutor;

import rx.schedulers.Schedulers;

@Component
public class AzureClientActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClientActions.class);

    @Inject
    private Azure azure;

    @Inject
    private SdxUtil sdxUtil;

    public List<String> listInstanceVolumeIds(List<String> instanceIds) {
        List<String> diskIds = new ArrayList<>();
        instanceIds.forEach(i -> {
            String resourceGroup = i.replaceFirst("..$", "");
            VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, i);
            diskIds.addAll(vm.dataDisks().values().stream().map(HasId::id).collect(Collectors.toList()));
        });
        return diskIds;
    }

    public void deleteInstances(List<String> instanceIds) {
        AzureInstanceActionExecutor.builder()
                .onInstances(instanceIds)
                .withInstanceAction(id -> {
                    String resourceGroup = id.replaceFirst("..$", "");
                    LOGGER.debug("Deleting vm {}", id);
                    return azure.virtualMachines().deleteByResourceGroupAsync(resourceGroup, id)
                            .doOnError(throwable -> LOGGER.debug("Error when stopping instance {}: {}", id, throwable))
                            .subscribeOn(Schedulers.io());
                }).withInstanceStatusCheck(id -> {
            String resourceGroup = id.replaceFirst("..$", "");
            VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, id);
            LOGGER.debug("After stop: vm {} is {} (expected null)", id, vm);
            return vm == null;
        }).withTimeout(10, TimeUnit.MINUTES).build().execute();
        LOGGER.debug("Deleting instances finished succesfully");
    }

    public void stopInstances(List<String> instanceIds) {
        AzureInstanceActionExecutor.builder()
                .onInstances(instanceIds)
                .withInstanceAction(id -> {
                    String resourceGroup = id.replaceFirst("..$", "");
                    VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, id);
                    LOGGER.debug("Before stop: vm {} power state is {}", id, vm.powerState());
                    return azure.virtualMachines().deallocateAsync(vm.resourceGroupName(), vm.name())
                            .doOnError(throwable -> LOGGER.debug("Error when stopping instance {}: {}", id, throwable))
                            .subscribeOn(Schedulers.io());
                }).withInstanceStatusCheck(id -> {
            String resourceGroup = id.replaceFirst("..$", "");
            VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, id);
            LOGGER.debug("After stop: vm {} power state is {}", id, vm.powerState());
            return PowerState.DEALLOCATED.equals(vm.powerState());
        }).withTimeout(10, TimeUnit.MINUTES).build().execute();
        LOGGER.debug("Stopping of instances finished succesfully");
    }
}
