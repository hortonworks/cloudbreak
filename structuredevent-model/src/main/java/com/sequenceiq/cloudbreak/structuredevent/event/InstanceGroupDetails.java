package com.sequenceiq.cloudbreak.structuredevent.event;

public class InstanceGroupDetails {
    // Kellenek-e a user adatok a resource-oknal, pl template? Lehet mas user hozta letre az accountban, pl...
    // Topology information ??

    private String groupName;

    private String groupType;

    private Integer nodeCount;

    private String instanceType;

    private String volumeType;

    private Integer volumeSize;

    private Integer volumeCount;

    private SecurityGroupDetails securityGroup;

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

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public Integer getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Integer volumeSize) {
        this.volumeSize = volumeSize;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public void setVolumeCount(Integer volumeCount) {
        this.volumeCount = volumeCount;
    }

    public SecurityGroupDetails getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupDetails securityGroup) {
        this.securityGroup = securityGroup;
    }
}
