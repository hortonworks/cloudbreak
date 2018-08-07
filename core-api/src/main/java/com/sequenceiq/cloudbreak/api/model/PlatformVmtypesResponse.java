package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVmtypesResponse implements JsonEntity {

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
