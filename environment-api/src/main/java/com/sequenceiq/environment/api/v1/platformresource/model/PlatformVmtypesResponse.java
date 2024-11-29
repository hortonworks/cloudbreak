package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVmtypesResponse implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, VirtualMachinesResponse> vmTypes = new HashMap<>();

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

    @Override
    public String toString() {
        return "PlatformVmtypesResponse{" +
                "vmTypes=" + vmTypes +
                '}';
    }
}
