package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualMachinesV4Response implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.VIRTUAL_MACHNES)
    private Set<VmTypeV4Response> virtualMachines = new HashSet<>();

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_VIRTUAL_MACHINES)
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
