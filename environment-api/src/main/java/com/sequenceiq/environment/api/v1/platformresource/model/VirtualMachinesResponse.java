package com.sequenceiq.environment.api.v1.platformresource.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualMachinesResponse extends AbstractVirtualMachinesResponse {

    @Override
    public String toString() {
        return "VirtualMachinesResponse{" + super.toString() + "}";
    }
}
