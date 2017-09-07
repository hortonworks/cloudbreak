package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVirtualMachinesJson implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.VIRTUAL_MACHNES)
    private Map<String, Collection<VmTypeJson>> virtualMachines = new HashMap<>();

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_VIRTUAL_MACHINES)
    private Map<String, String> defaultVirtualMachines = new HashMap<>();

    @ApiModelProperty(ConnectorModelDescription.VIRTUAL_MACHINES_PER_ZONES)
    private Map<String, Map<String, Collection<VmTypeJson>>> vmTypesPerZones = new HashMap<>();

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_VIRTUAL_MACHINES_PER_ZONES)
    private Map<String, Map<String, String>> defaultVmTypePerZones = new HashMap<>();

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

    public Map<String, Map<String, Collection<VmTypeJson>>> getVmTypesPerZones() {
        return vmTypesPerZones;
    }

    public void setVmTypesPerZones(Map<String, Map<String, Collection<VmTypeJson>>> vmTypesPerZones) {
        this.vmTypesPerZones = vmTypesPerZones;
    }

    public Map<String, Map<String, String>> getDefaultVmTypePerZones() {
        return defaultVmTypePerZones;
    }

    public void setDefaultVmTypePerZones(Map<String, Map<String, String>> defaultVmTypePerZones) {
        this.defaultVmTypePerZones = defaultVmTypePerZones;
    }
}
