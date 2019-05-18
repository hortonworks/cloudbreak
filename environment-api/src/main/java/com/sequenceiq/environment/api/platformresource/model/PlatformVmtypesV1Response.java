package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVmtypesV1Response implements Serializable {

    private Map<String, VirtualMachinesV1Response> vmTypes;

    public PlatformVmtypesV1Response() {
    }

    public PlatformVmtypesV1Response(Map<String, VirtualMachinesV1Response> vmTypes) {
        this.vmTypes = vmTypes;
    }

    public Map<String, VirtualMachinesV1Response> getVmTypes() {
        return vmTypes;
    }

    public void setVmTypes(Map<String, VirtualMachinesV1Response> vmTypes) {
        this.vmTypes = vmTypes;
    }
}
