package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceGroupDetails implements Serializable {
    private String groupName;

    private String groupType;

    private Integer nodeCount;

    private String instanceType;

    private List<VolumeDetails> volumes;

    private SecurityGroupDetails securityGroup;

    private String temporaryStorage;

    private Integer rootVolumeSize;

    private Map<String, Object> attributes;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public List<VolumeDetails> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeDetails> volumes) {
        this.volumes = volumes;
    }

    public SecurityGroupDetails getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupDetails securityGroup) {
        this.securityGroup = securityGroup;
    }

    public String getTemporaryStorage() {
        return temporaryStorage;
    }

    public void setTemporaryStorage(String temporaryStorage) {
        this.temporaryStorage = temporaryStorage;
    }

    public Integer getRootVolumeSize() {
        return rootVolumeSize;
    }

    public void setRootVolumeSize(Integer rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
