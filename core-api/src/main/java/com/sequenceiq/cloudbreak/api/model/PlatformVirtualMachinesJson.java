package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVirtualMachinesJson implements JsonEntity {

    private Map<String, Collection<VmTypeJson>> virtualMachines = new HashMap<>();
    private Map<String, String> defaultVirtualMachines = new HashMap<>();

    public Map<String, Collection<VmTypeJson>> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Map<String, Collection<VmTypeJson>> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public Map<String, String> getDefaultVirtualMachines() {
        return defaultVirtualMachines;
    }

    public void setDefaultVirtualMachines(Map<String, String> defaultVirtualMachines) {
        this.defaultVirtualMachines = defaultVirtualMachines;
    }


}
