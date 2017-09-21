package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVmtypesResponse implements JsonEntity {

    private Map<String, VirtualMachinesResponse> vmTypes = new HashMap<>();

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
