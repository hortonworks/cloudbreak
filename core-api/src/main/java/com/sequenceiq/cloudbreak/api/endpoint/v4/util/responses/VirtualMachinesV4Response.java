package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualMachinesV4Response implements JsonEntity {

    @Schema(description = ConnectorModelDescription.VIRTUAL_MACHNES)
    private Set<VmTypeV4Response> virtualMachines = new HashSet<>();

    @Schema(description = ConnectorModelDescription.DEFAULT_VIRTUAL_MACHINES)
    private VmTypeV4Response defaultVirtualMachine;

    public Set<VmTypeV4Response> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VmTypeV4Response> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public VmTypeV4Response getDefaultVirtualMachine() {
        return defaultVirtualMachine;
    }

    public void setDefaultVirtualMachine(VmTypeV4Response defaultVirtualMachine) {
        this.defaultVirtualMachine = defaultVirtualMachine;
    }
}
