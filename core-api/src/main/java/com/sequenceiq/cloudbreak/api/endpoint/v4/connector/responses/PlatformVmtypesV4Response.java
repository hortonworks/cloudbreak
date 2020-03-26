package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VirtualMachinesV4Response;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVmtypesV4Response implements JsonEntity {

    private Map<String, VirtualMachinesV4Response> vmTypes;

    public PlatformVmtypesV4Response() {
    }

    public PlatformVmtypesV4Response(Map<String, VirtualMachinesV4Response> vmTypes) {
        this.vmTypes = vmTypes;
    }

    public Map<String, VirtualMachinesV4Response> getVmTypes() {
        return vmTypes;
    }

    public void setVmTypes(Map<String, VirtualMachinesV4Response> vmTypes) {
        this.vmTypes = vmTypes;
    }
}
