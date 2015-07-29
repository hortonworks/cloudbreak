package com.sequenceiq.cloudbreak.service.stack.flow;

public class CoreInstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicIp;
    private Integer volumeCount;
    private Integer containerCount = 0;
    private String instanceGroupName;

    public CoreInstanceMetaData() {
    }

    public CoreInstanceMetaData(String instanceId, String privateIp, String publicIp, Integer volumeCount, String instanceGroupName) {
        this.instanceId = instanceId;
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.volumeCount = volumeCount;
        this.instanceGroupName = instanceGroupName;
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

    public Integer getContainerCount() {
        return containerCount;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }
}
