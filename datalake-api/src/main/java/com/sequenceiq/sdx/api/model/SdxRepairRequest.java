package com.sequenceiq.sdx.api.model;

import java.util.List;

import javax.validation.constraints.NotEmpty;

public class SdxRepairRequest {

    @NotEmpty(message = "Please specify at least one hostgroup to repair")
    private List<String> hostGroupNames;

    public List<String> getHostGroupNames() {
        return hostGroupNames;
    }

    public void setHostGroupNames(List<String> hostGroupNames) {
        this.hostGroupNames = hostGroupNames;
    }
}