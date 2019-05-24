package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVmtypesResponse implements Serializable {

    private Map<String, VirtualMachinesResponse> vmTypes;

    public PlatformVmtypesResponse() {
    }

    public PlatformVmtypesResponse(Map<String, VirtualMachinesResponse> vmTypes) {
        this.vmTypes = vmTypes;
    }

    public Map<String, VirtualMachinesResponse> getVmTypes() {
        return vmTypes;
    }

    public void setVmTypes(Map<String, VirtualMachinesResponse> vmTypes) {
        this.vmTypes = vmTypes;
    }
}
