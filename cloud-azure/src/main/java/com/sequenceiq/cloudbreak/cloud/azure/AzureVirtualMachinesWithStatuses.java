package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.Map;

import com.microsoft.azure.management.compute.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class AzureVirtualMachinesWithStatuses {

    private Map<String, VirtualMachine> virtualMachines;

    private List<CloudVmInstanceStatus> statuses;

    public AzureVirtualMachinesWithStatuses(Map<String, VirtualMachine> virtualMachines, List<CloudVmInstanceStatus> statuses) {
        this.virtualMachines = virtualMachines;
        this.statuses = statuses;
    }

    public Map<String, VirtualMachine> getVirtualMachines() {
        return virtualMachines;
    }

    public List<CloudVmInstanceStatus> getStatuses() {
        return statuses;
    }
}
