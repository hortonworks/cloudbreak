package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.VirtualMachinesResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVmtypesV4Response implements JsonEntity {

    private Map<String, VirtualMachinesResponse> vmTypes;

    public PlatformVmtypesV4Response() {
    }

    public PlatformVmtypesV4Response(Map<String, VirtualMachinesResponse> vmTypes) {
        this.vmTypes = vmTypes;
    }

    public Map<String, VirtualMachinesResponse> getVmTypes() {
        return vmTypes;
    }

    public void setVmTypes(Map<String, VirtualMachinesResponse> vmTypes) {
        this.vmTypes = vmTypes;
    }
}
