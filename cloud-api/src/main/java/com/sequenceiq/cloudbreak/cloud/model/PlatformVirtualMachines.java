package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PlatformVirtualMachines {

    private final Map<Platform, Collection<VmType>> virtualMachines;

    private final Map<Platform, VmType> defaultVirtualMachines;

    private final Map<Platform, Map<AvailabilityZone, Collection<VmType>>> vmTypesPerZones;

    private final Map<Platform, Map<AvailabilityZone, VmType>> defaultVmTypePerZones;

    public PlatformVirtualMachines(Map<Platform, Collection<VmType>> virtualMachines, Map<Platform, VmType> defaultVirtualMachines,
            Map<Platform, Map<AvailabilityZone, Collection<VmType>>> vmPerZones, Map<Platform, Map<AvailabilityZone, VmType>> defaultVmPerZones) {
        this.virtualMachines = virtualMachines;
        this.defaultVirtualMachines = defaultVirtualMachines;
        this.vmTypesPerZones = vmPerZones;
        this.defaultVmTypePerZones = defaultVmPerZones;
    }

    public PlatformVirtualMachines() {
        this.virtualMachines = new HashMap<>();
        this.defaultVirtualMachines = new HashMap<>();
        this.vmTypesPerZones = new HashMap<>();
        this.defaultVmTypePerZones = new HashMap<>();
    }

    public Map<Platform, Collection<VmType>> getVirtualMachines() {
        return virtualMachines;
    }

    public Map<Platform, VmType> getDefaultVirtualMachines() {
        return defaultVirtualMachines;
    }

    public Map<Platform, Map<AvailabilityZone, Collection<VmType>>> getVmTypesPerZones() {
        return vmTypesPerZones;
    }

    public Map<Platform, Map<AvailabilityZone, VmType>> getDefaultVmTypePerZones() {
        return defaultVmTypePerZones;
    }
}
