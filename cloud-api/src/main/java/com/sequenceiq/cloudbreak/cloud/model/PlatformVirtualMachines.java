package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PlatformVirtualMachines {

    private final Map<Platform, Collection<VmType>> virtualMachines;

    private final Map<Platform, VmType> defaultVirtualMachines;

    public PlatformVirtualMachines(Map<Platform, Collection<VmType>> virtualMachines, Map<Platform, VmType> defaultVirtualMachines) {
        this.virtualMachines = virtualMachines;
        this.defaultVirtualMachines = defaultVirtualMachines;
    }

    public PlatformVirtualMachines() {
        this.virtualMachines = new HashMap<>();
        this.defaultVirtualMachines = new HashMap<>();
    }

    public Map<Platform, Collection<VmType>> getVirtualMachines() {
        return virtualMachines;
    }

    public Map<Platform, VmType> getDefaultVirtualMachines() {
        return defaultVirtualMachines;
    }
}
