package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.platformresource.PlatformResourceModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualMachinesV1Response implements Serializable {

    @ApiModelProperty(PlatformResourceModelDescription.VIRTUAL_MACHNES)
    private Set<VmTypeV1Response> virtualMachines = new HashSet<>();

    @ApiModelProperty(PlatformResourceModelDescription.DEFAULT_VIRTUAL_MACHINES)
    private VmTypeV1Response defaultVirtualMachine;

    public Set<VmTypeV1Response> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VmTypeV1Response> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public VmTypeV1Response getDefaultVirtualMachine() {
        return defaultVirtualMachine;
    }

    public void setDefaultVirtualMachine(VmTypeV1Response defaultVirtualMachine) {
        this.defaultVirtualMachine = defaultVirtualMachine;
    }
}
