package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractVirtualMachinesResponse implements Serializable {

    @Schema(description = PlatformResourceModelDescription.VIRTUAL_MACHINES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<VmTypeResponse> virtualMachines = new HashSet<>();

    @Schema(description = PlatformResourceModelDescription.DEFAULT_VIRTUAL_MACHINES)
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
        return "AbstractVirtualMachinesResponse{" +
                "virtualMachines=" + virtualMachines +
                ", defaultVirtualMachine=" + defaultVirtualMachine +
                '}';
    }
}
