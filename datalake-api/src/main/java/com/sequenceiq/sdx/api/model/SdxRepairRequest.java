package com.sequenceiq.sdx.api.model;

import java.util.List;

public class SdxRepairRequest {

    private String hostGroupName;

    private List<String> hostGroupNames;

    public String getHostGroupName() {
        return hostGroupName;
    }

    public void setHostGroupName(String hostGroupName) {
        this.hostGroupName = hostGroupName;
    }

    public List<String> getHostGroupNames() {
        return hostGroupNames;
    }

    public void setHostGroupNames(List<String> hostGroupNames) {
        this.hostGroupNames = hostGroupNames;
    }
}