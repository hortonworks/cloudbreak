package com.sequenceiq.provisioning.controller.json;

import java.util.List;

//TODO: validate that the sum cardinality in the hostGroups is the same as the nodeCount in the stack
public class ClusterRequest {

    private String clusterName;
    private Long blueprintId;
    private List<HostGroupMappingJson> hostGroups;
    private String ambariIp;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public List<HostGroupMappingJson> getHostGroups() {
        return hostGroups;
    }

    public void setHosts(List<HostGroupMappingJson> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public void setHostGroups(List<HostGroupMappingJson> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

}
