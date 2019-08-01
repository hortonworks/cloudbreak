package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

public class SdxRepairRequest {

    @NotNull
    private String hostGroupName;

    public String getHostGroupName() {
        return hostGroupName;
    }

    public void setHostGroupName(String hostGroupName) {
        this.hostGroupName = hostGroupName;
    }
}