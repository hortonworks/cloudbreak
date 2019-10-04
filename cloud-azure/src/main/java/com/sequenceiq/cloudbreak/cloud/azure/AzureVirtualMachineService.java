package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

@Component
class AzureVirtualMachineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureVirtualMachineService.class);

    Map<String, VirtualMachine> getVirtualMachinesByName(AzureClient azureClient, String resourceGroup, Set<String> privateInstanceIds) {
        LOGGER.debug("Starting to retrieve vm metadata from Azure.");
        PagedList<VirtualMachine> virtualMachines = azureClient.getVirtualMachines(resourceGroup);
        while (virtualMachines.hasNextPage() && hasMissingVm(virtualMachines, privateInstanceIds)) {
            virtualMachines.loadNextPage();
        }
        validateResponse(virtualMachines, privateInstanceIds);
        return collectVirtualMachinesByName(privateInstanceIds, virtualMachines);
    }

    private boolean hasMissingVm(PagedList<VirtualMachine> virtualMachines, Set<String> privateInstanceIds) {
        Set<String> virtualMachineNames = virtualMachines.stream().map(VirtualMachine::name).collect(Collectors.toSet());
        return !virtualMachineNames.containsAll(privateInstanceIds);
    }

    private Map<String, VirtualMachine> collectVirtualMachinesByName(Set<String> privateInstanceIds, PagedList<VirtualMachine> virtualMachines) {
        return virtualMachines.stream()
                .filter(virtualMachine -> privateInstanceIds.contains(virtualMachine.name()))
                .collect(Collectors.toMap(HasName::name, vm -> vm));
    }

    private void validateResponse(PagedList<VirtualMachine> virtualMachines, Set<String> privateInstanceIds) {
        if (hasMissingVm(virtualMachines, privateInstanceIds)) {
            LOGGER.warn("Failed to retrieve all host from Azure. Only {} found from the {}.", virtualMachines.size(), privateInstanceIds.size());
        }
    }
}
