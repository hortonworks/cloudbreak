package com.sequenceiq.environment.api.v1.platformresource.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseVirtualMachinesResponse extends VirtualMachinesResponse {

    @Override
    public String toString() {
        return "DatabaseVirtualMachinesResponse{" + super.toString() + "}";
    }
}
