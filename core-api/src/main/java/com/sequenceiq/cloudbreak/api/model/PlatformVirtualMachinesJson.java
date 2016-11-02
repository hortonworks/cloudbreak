package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVirtualMachinesJson implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.VIRTUAL_MACHNES)
    private Map<String, Collection<VmTypeJson>> virtualMachines = new HashMap<>();

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.DEFAULT_VIRTUAL_MACHINES)
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
