package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.domain.InstanceGroup;

public class CoreInstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicIp;
    private Integer volumeCount;
    private String longName;
    private Integer containerCount = 0;
    private InstanceGroup instanceGroup;

    public CoreInstanceMetaData() {
    }

    public CoreInstanceMetaData(String instanceId, String privateIp, String publicIp, Integer volumeCount, String longName, InstanceGroup instanceGroup) {
        this.instanceId = instanceId;
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.volumeCount = volumeCount;
        this.longName = longName;
        this.instanceGroup = instanceGroup;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public String getLongName() {
        return longName;
    }

    public Integer getContainerCount() {
        return containerCount;
    }

    public InstanceGroup getInstanceGroup() {
        return instanceGroup;
    }
}
