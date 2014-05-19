package com.sequenceiq.provisioning.controller.json;

import java.util.List;

//TODO: validate that the sum cardinality in the hostGroups is the same as the nodeCount in the cloudInstance
public class ClusterJson {

    String clusterName;
    String blueprintName;
    List<HostGroupMappingJson> hostGroups;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public List<HostGroupMappingJson> getHostGroups() {
        return hostGroups;
    }

    public void setHosts(List<HostGroupMappingJson> hostGroups) {
        this.hostGroups = hostGroups;
    }

}
