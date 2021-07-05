package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualMachinesResponse implements Serializable {

    @ApiModelProperty(PlatformResourceModelDescription.VIRTUAL_MACHNES)
    private Set<VmTypeResponse> virtualMachines = new HashSet<>();

    @ApiModelProperty(PlatformResourceModelDescription.DEFAULT_VIRTUAL_MACHINES)
    private VmTypeResponse defaultVirtualMachine;

    public Set<VmTypeResponse> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VmTypeResponse> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public VmTypeResponse getDefaultVirtualMachine() {
        return defaultVirtualMachine;
    }

    public void setDefaultVirtualMachine(VmTypeResponse defaultVirtualMachine) {
        this.defaultVirtualMachine = defaultVirtualMachine;
    }

    @Override
    public String toString() {
        return "VirtualMachinesResponse{" +
                "virtualMachines=" + virtualMachines +
                ", defaultVirtualMachine=" + defaultVirtualMachine +
                '}';
    }
}
