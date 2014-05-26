package com.sequenceiq.provisioning.controller.json;

import java.util.List;

//TODO: validate that the sum cardinality in the hostGroups is the same as the nodeCount in the stack
public class ClusterRequest {

    private String clusterName;
    private String blueprintId;
    private List<HostGroupMappingJson> hostGroups;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(String blueprintId) {
        this.blueprintId = blueprintId;
    }

    public List<HostGroupMappingJson> getHostGroups() {
        return hostGroups;
    }

    public void setHosts(List<HostGroupMappingJson> hostGroups) {
        this.hostGroups = hostGroups;
    }

}
