package com.sequenceiq.it.cloudbreak.util.azure.azurevm.action;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.models.PowerState;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineDataDisk;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

@Component
public class AzureClientActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClientActions.class);

    @Inject
    private AzureResourceManager azure;

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

    public List<String> listInstanceTypes(String clusterName, List<String> instanceIds) {
        return instanceIds.stream().map(
                id ->  {
                    String resourceGroup = getResourceGroupName(clusterName, id);
                    return Optional.ofNullable(azure.virtualMachines()).orElseThrow()
                            .getByResourceGroup(resourceGroup, id).size().toString().toLowerCase();
                }
        ).collect(Collectors.toList());
    }

    public void deleteInstances(String clusterName, List<String> instanceIds) {
        if (!instanceIds.isEmpty()) {
            String resourceGroup = getResourceGroupName(clusterName, instanceIds.get(0));
            try {
                List<String> vmIdList = getVmIdList(instanceIds, resourceGroup);
                azure.virtualMachines().deleteByIds(vmIdList);

                List<VirtualMachine> vmList = getVmList(instanceIds, resourceGroup);
                if (!vmList.isEmpty()) {
                    String errorMsg = String.format("There are not deleted instances: %s, retrying..", vmList.stream().map(VirtualMachine::id));
                    LOGGER.warn(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                LOGGER.info("Deleting instances finished successfully");
            } catch (ManagementException e) {
                LOGGER.debug("Exception occurred during the deletion of Virtual Machines by resource group, ignoring it", e);
            }
        } else {
            LOGGER.info("Instance id list is empty, nothing to delete for {} ", clusterName);
        }
    }

    private boolean isVmDeallocated(VirtualMachine vm) {
        String powerState = getVmPowerState(vm);
        String provisioningState = getVmProvisioningState(vm);
        boolean success = PowerState.DEALLOCATED.toString().equals(powerState);
        Log.log("Stop action isSuccessful={}, vm {} power state is {}, provisioning state is {}", success, vm.id(), powerState, provisioningState);
        return success;
    }

    private List<String> getVmIdList(List<String> instanceIds, String resourceGroup) {
        return getVmList(instanceIds, resourceGroup).stream()
                .map(VirtualMachine::id)
                .collect(Collectors.toList());
    }

    private List<VirtualMachine> getVmList(List<String> instanceIds, String resourceGroup) {
        PagedIterable<VirtualMachine> virtualMachines = azure.virtualMachines().listByResourceGroup(resourceGroup);
        List<VirtualMachine> filteredVmList = virtualMachines.stream()
                .filter(Objects::nonNull)
                .filter(vm -> instanceIds.contains(vm.name()))
                .collect(Collectors.toList());
        LOGGER.debug("The following VMs are found in resource group {}: {}", resourceGroup, filteredVmList);
        return filteredVmList;
    }

    public void stopInstances(String clusterName, List<String> instanceIds) {
        if (!instanceIds.isEmpty()) {
            String resourceGroup = getResourceGroupName(clusterName, instanceIds.get(0));
            try {
//                List<String> vmIdList = getVmIdList(instanceIds, resourceGroup);
                instanceIds.forEach(vm -> azure.virtualMachines().deallocate(resourceGroup, vm));

                List<VirtualMachine> vmList = getVmList(instanceIds, resourceGroup);

                vmList.forEach(vm -> {
                    String powerState = getVmPowerState(vm);
                    String provisioningState = getVmProvisioningState(vm);
                    boolean success = PowerState.DEALLOCATED.toString().equals(powerState);
                    Log.log("Stop action isSuccessful={}, vm {} power state is {}, provisioning state is {}", success, vm.name(), powerState, provisioningState);
                    if (!success) {
                        String errorMsg = String.format("There is at least one not stopped instance: %s, retrying..", vm.name());
                        LOGGER.warn(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                });
                LOGGER.info("Stopping instances finished successfully");
            } catch (ManagementException e) {
                LOGGER.debug("Exception occurred during the stopping of Virtual Machines by resource group, ignoring it", e);
            }
        } else {
            LOGGER.info("Instance id list is empty, nothing to stop for {} ", clusterName);
        }
    }

    private String getVmPowerState(VirtualMachine vm) {
        return Optional.ofNullable(vm).map(VirtualMachine::powerState).map(PowerState::toString).orElse("no power state");
    }

    private String getVmProvisioningState(VirtualMachine vm) {
        return Optional.ofNullable(vm).map(VirtualMachine::provisioningState).orElse("no provisioning state");
    }

    // If we don't have resource group name, we are going to be guessing it from cluster name and instance name.
    private String getResourceGroupName(String clusterName, String id) {
        String resourcegroupName = azureProperties.getResourcegroup().getName();
        return StringUtils.isBlank(resourcegroupName) ? id.replaceAll("(" + clusterName + "[0-9]+).*", "$1") : resourcegroupName;
    }

    public void setAzure(AzureResourceManager azure) {
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
                                        .innerModel()
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
            if (!resourceGroupExists(resourceGroupName)) {
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
                LOGGER.warn(format("Resource group already exist! So creating new resource group with name '%s' is not necessary.",
                        resourceGroupName));
                return azure.resourceGroups().getByName(resourceGroupName);
            }
        } else {
            LOGGER.error("Resource group name has not been provided! So creating new resource group for test is not possible. " +
                    "Please set a valid Azure resource group name at 'integrationtest.azure.resourcegroup.name' variable in the 'application.yml'" +
                    " or as environment variable!");
            throw new TestFailException("Resource group name has not been provided! So creating new resource group for test is not possible. " +
                    "Please set a valid Azure resource group name at 'integrationtest.azure.resourcegroup.name' variable in the 'application.yml'" +
                    " or as environment variable!");
        }
    }

    public void deleteResourceGroup(String resourceGroupName) {
        if (StringUtils.isNotBlank(resourceGroupName) && resourceGroupExists(resourceGroupName)) {
            LOGGER.info(format("Removing resource group '%s'...", resourceGroupName));
            azure.resourceGroups().deleteByName(resourceGroupName);
        } else {
            LOGGER.info(format("Resource group ('%s') has already been removed at Azure or name is null!", resourceGroupName));
        }
    }

    private boolean resourceGroupExists(String resourceGroupName) {
        boolean exists = azure.resourceGroups().contain(resourceGroupName);
        if (exists) {
            LOGGER.info(format("Found resource group with name '%s'", resourceGroupName));
            return true;
        } else {
            LOGGER.info(format("Resource group '%s' is not present", resourceGroupName));
            return false;
        }
    }
}
