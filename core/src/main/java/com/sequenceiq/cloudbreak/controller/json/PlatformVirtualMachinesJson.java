package com.sequenceiq.cloudbreak.controller.json;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVirtualMachinesJson implements JsonEntity {

    private Map<String, Collection<VmTypeJson>> virtualMachines;
    private Map<String, String> defaultVirtualMachines;

    public PlatformVirtualMachinesJson(Map<String, Collection<VmTypeJson>> virtualMachines, Map<String, String> defaultVirtualMachines) {
        this.virtualMachines = virtualMachines;
        this.defaultVirtualMachines = defaultVirtualMachines;
    }

    public PlatformVirtualMachinesJson() {
        this.virtualMachines = new HashMap<>();
        this.defaultVirtualMachines = new HashMap<>();
    }

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
