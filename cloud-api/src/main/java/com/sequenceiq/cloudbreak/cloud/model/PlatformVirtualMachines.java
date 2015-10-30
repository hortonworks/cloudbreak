package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class PlatformVirtualMachines {
    private Map<String, Map<String, String>> virtualMachines;
    private Map<String, String> defaultVirtualMachines;

    public PlatformVirtualMachines(Map<String, Map<String, String>> virtualMachines, Map<String, String> defaultVirtualMachines) {
        this.virtualMachines = virtualMachines;
        this.defaultVirtualMachines = defaultVirtualMachines;
    }

    public PlatformVirtualMachines() {
        this.virtualMachines = new HashMap<>();
        this.defaultVirtualMachines = new HashMap<>();
    }

    public Map<String, Map<String, String>> getVirtualMachines() {
        return virtualMachines;
    }

    public Map<String, String> getDefaultVirtualMachines() {
        return defaultVirtualMachines;
    }
}
