package com.sequenceiq.sdx.api.model;

import java.util.List;

public class SdxRepairRequest {

    private String hostGroupName;

    private List<String> hostGroupNames;

    private List<String> nodesIds;

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

    public List<String> getNodesIds() {
        return nodesIds;
    }

    public void setNodesIds(List<String> nodesIds) {
        this.nodesIds = nodesIds;
    }

    @Override
    public String toString() {
        return "SdxRepairRequest{" +
                "hostGroupName='" + hostGroupName + '\'' +
                ", hostGroupNames=" + hostGroupNames +
                ", nodesIds=" + nodesIds +
                '}';
    }
}