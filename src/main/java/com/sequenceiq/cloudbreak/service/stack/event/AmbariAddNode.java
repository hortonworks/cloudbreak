package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;

public class AmbariAddNode {

    private Long stackId;
    private String ambariIp;
    private Set<InstanceMetaData> newNodesInstanceMetaData;
    private Set<Resource> resources;
    private String hostgroup;

    public AmbariAddNode(Long stackId, String ambariIp, Set<InstanceMetaData> newNodesInstanceMetaData, Set<Resource> resources, String hostgroup) {
        this.stackId = stackId;
        this.ambariIp = ambariIp;
        this.newNodesInstanceMetaData = newNodesInstanceMetaData;
        this.resources = resources;
        this.hostgroup = hostgroup;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public String getHostgroup() {
        return hostgroup;
    }

    public void setHostgroup(String hostgroup) {
        this.hostgroup = hostgroup;
    }

    public Set<InstanceMetaData> getNewNodesInstanceMetaData() {
        return newNodesInstanceMetaData;
    }

    public void setNewNodesInstanceMetaData(Set<InstanceMetaData> newNodesInstanceMetaData) {
        this.newNodesInstanceMetaData = newNodesInstanceMetaData;
    }
}
