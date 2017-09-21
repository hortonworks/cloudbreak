package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualMachinesResponse implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.VIRTUAL_MACHNES)
    private Set<VmTypeJson> virtualMachines = new HashSet<>();

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_VIRTUAL_MACHINES)
    private VmTypeJson defaultVirtualMachine;

    public Set<VmTypeJson> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VmTypeJson> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public VmTypeJson getDefaultVirtualMachine() {
        return defaultVirtualMachine;
    }

    public void setDefaultVirtualMachine(VmTypeJson defaultVirtualMachine) {
        this.defaultVirtualMachine = defaultVirtualMachine;
    }
}
