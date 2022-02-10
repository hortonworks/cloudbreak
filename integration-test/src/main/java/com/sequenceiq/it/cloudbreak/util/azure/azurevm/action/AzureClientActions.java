package com.sequenceiq.it.cloudbreak.util.azure.azurevm.action;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineDataDisk;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasId;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.azure.AzureInstanceActionExecutor;
import com.sequenceiq.it.cloudbreak.util.azure.AzureInstanceActionResult;

import rx.schedulers.Schedulers;

@Component
public class AzureClientActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClientActions.class);

    @Inject
    private Azure azure;

    @Inject
    private AzureProperties azureProperties;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public List<String> listInstanceVolumeIds(String clusterName, List<String> instanceIds) {
        List<String> diskIds = new ArrayList<>();
        instanceIds.forEach(id -> {
            String resourceGroup = getResourceGroupName(clusterName, id);
            VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, id);
            Map<Integer, VirtualMachineDataDisk> dataDiskMap = vm.dataDisks();
            if (dataDiskMap != null && !dataDiskMap.isEmpty()) {
                diskIds.addAll(dataDiskMap.values().stream().map(HasId::id).collect(Collectors.toList()));
                LOGGER.info("Instance '{}' has attached volumes [{}].", id, diskIds);
            }
        });
        return diskIds;
    }

    public void deleteInstances(String clusterName, List<String> instanceIds) {
        AzureInstanceActionExecutor.builder()
                .onInstances(instanceIds)
                .withInstanceAction(id -> {
                    String resourceGroup = getResourceGroupName(clusterName, id);
                    VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, id);
                    LOGGER.info("Before deleting, vm {} power state is {}", id, vm.powerState());
                    return azure.virtualMachines().deleteByResourceGroupAsync(resourceGroup, id)
                            .doOnError(throwable -> Log.error(LOGGER, "Error when deleting instance {}: {}", id, throwable))
                            .subscribeOn(Schedulers.io());
                })
                .withInstanceStatusCheck(id -> {
                    String resourceGroup = getResourceGroupName(clusterName, id);
                    VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, id);
                    String powerState = getVmPowerState(vm);
                    String provisioningState = getVmProvisioningState(vm);
                    Log.log("Delete action is successful={} (expected true), vm {} power state is {}, provisioning state is {}",
                            vm == null, id, powerState, provisioningState);
                    return new AzureInstanceActionResult(vm == null, powerState, id);
                })
                .withTimeout(10, TimeUnit.MINUTES)
                .build()
                .execute();
        LOGGER.info("Deleting instances finished succesfully");
    }

    public void stopInstances(String clusterName, List<String> instanceIds) {
        AzureInstanceActionExecutor.builder()
                .onInstances(instanceIds)
                .withInstanceAction(id -> {
                    String resourceGroup = getResourceGroupName(clusterName, id);
                    VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, id);
                    LOGGER.info("Before stopping, vm {} power state is {}", id, vm.powerState());
                    return azure.virtualMachines().deallocateAsync(vm.resourceGroupName(), vm.name())
                            .doOnError(throwable -> Log.error(LOGGER, "Error when stopping instance {}: {}", id, throwable))
                            .subscribeOn(Schedulers.io());
                })
                .withInstanceStatusCheck(id -> {
                    String resourceGroup = getResourceGroupName(clusterName, id);
                    VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, id);
                    String powerState = getVmPowerState(vm);
                    String provisioningState = getVmProvisioningState(vm);
                    boolean success = PowerState.DEALLOCATED.toString().equals(powerState);
                    Log.log("Stop action isSuccessful={}, vm {} power state is {}, provisioning state is {}", success, id, powerState, provisioningState);
                    return new AzureInstanceActionResult(success, powerState, id);
                })
                .withTimeout(10, TimeUnit.MINUTES)
                .build()
                .execute();
        LOGGER.info("Stopping of instances finished succesfully");
    }

    private String getVmPowerState(VirtualMachine vm) {
        return Optional.ofNullable(vm).map(VirtualMachine::powerState).map(PowerState::toString).orElse("no power state");
    }

    private String getVmProvisioningState(VirtualMachine vm) {
        return Optional.ofNullable(vm).map(VirtualMachine::provisioningState).orElse("no provisioning state");
    }

    // We don't have resource group name, so we are guessing it from cluster name and instance name
    private String getResourceGroupName(String clusterName, String id) {
        return id.replaceAll("(" + clusterName + "[0-9]+).*", "$1");
    }

    public void setAzure(Azure azure) {
        this.azure = azure;
    }

    public Map<String, Map<String, String>> listTagsByInstanceId(String clusterName, List<String> instanceIds) {
        return instanceIds.stream()
                .filter(StringUtils::isNotEmpty)
                .map(id -> azure.virtualMachines().getByResourceGroup(getResourceGroupName(clusterName, id), id))
                .collect(Collectors.toMap(VirtualMachine::id, VirtualMachine::tags));
    }

    public List<String> getVolumesDesId(String clusterName, String resourceGroupName, List<String> instanceIds) {
        List<String> diskEncryptionSetIds = new ArrayList<>();

        instanceIds.forEach(id -> {
            VirtualMachine virtualMachine;
            if (StringUtils.isBlank(resourceGroupName)) {
                virtualMachine = azure.virtualMachines().getByResourceGroup(getResourceGroupName(clusterName, id), id);
            } else {
                virtualMachine = azure.virtualMachines().getByResourceGroup(resourceGroupName, id);
            }
            Map<Integer, VirtualMachineDataDisk> dataDiskMap = virtualMachine.dataDisks();

            if (MapUtils.isNotEmpty(dataDiskMap)) {
                Map<String, String> volumeIdDesIdMap = dataDiskMap.values()
                        .stream()
                        .collect(Collectors.toMap(HasId::id, virtualMachineDataDisk ->
                                virtualMachineDataDisk
                                        .inner()
                                        .managedDisk()
                                        .diskEncryptionSet()
                                        .id()));
                LOGGER.info("Instance '{}' has attached volumes [{}] and disk encryption sets [{}].", id, volumeIdDesIdMap.keySet(), volumeIdDesIdMap.values());
                diskEncryptionSetIds.addAll(volumeIdDesIdMap.values());
            }
        });
        return diskEncryptionSetIds;
    }

    public ResourceGroup createResourceGroup(String resourceGroupName, Map<String, String> tags) {
        if (StringUtils.isNotBlank(resourceGroupName)) {
            LOGGER.info(format("Creating resource group '%s'...", resourceGroupName));
            Map<String, String> allTags = new HashMap<>();
            allTags.putAll(tags);
            allTags.putAll(commonCloudProperties.getTags());
            ResourceGroup resourceGroup;
            resourceGroup = azure.resourceGroups().define(resourceGroupName)
                    .withRegion(azureProperties.getRegion())
                    .withTags(allTags)
                    .create();
            if (resourceGroup.provisioningState().equalsIgnoreCase("Succeeded")) {
                LOGGER.info(format("New resource group '%s' has been created.", resourceGroupName));
                Log.then(LOGGER, format(" New resource group '%s' has been created. ", resourceGroupName));
                return resourceGroup;
            } else {
                LOGGER.error("Failed to provision the resource group '{}'!", resourceGroupName);
                throw new TestFailException(format("Failed to provision the resource group '%s'!", resourceGroupName));
            }
        } else {
            LOGGER.error("Resource group name has not been provided! So create new resource group for environment is not possible.");
            throw new TestFailException("Resource group name has not been provided! So create new resource group for environment is not possible.");
        }
    }

    public void deleteResourceGroup(String resourceGroupName) {
        if (StringUtils.isNotBlank(resourceGroupName) && resourceGroupExists(resourceGroupName)) {
            LOGGER.info(String.format("Removing resource group '%s'...", resourceGroupName));
            azure.resourceGroups().deleteByName(resourceGroupName);
        } else {
            LOGGER.info(String.format("Resource group ('%s') has already been removed at Azure or name is null!", resourceGroupName));
        }
    }

    private boolean resourceGroupExists(String resourceGroupName) {
        boolean exists = azure.resourceGroups().contain(resourceGroupName);
        if (exists) {
            LOGGER.info(String.format("Found resource group with name '%s'", resourceGroupName));
            return true;
        } else {
            LOGGER.info(String.format("Resource group '%s' is not present", resourceGroupName));
            return false;
        }
    }
}
