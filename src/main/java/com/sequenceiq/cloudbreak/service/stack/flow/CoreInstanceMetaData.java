package com.sequenceiq.cloudbreak.service.stack.flow;

public class CoreInstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicIp;
    private Integer volumeCount;
    private String longName;
    private Integer containerCount = 0;

    public CoreInstanceMetaData() {
    }

    public CoreInstanceMetaData(String instanceId, String privateIp, String publicIp, Integer volumeCount, String longName, Integer containerCount) {
        this.instanceId = instanceId;
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.volumeCount = volumeCount;
        this.longName = longName;
        this.containerCount = containerCount;
    }

    public CoreInstanceMetaData(String instanceId, String privateIp, String publicIp, Integer volumeCount, String longName) {
        this(instanceId, privateIp, publicIp, volumeCount, longName, 0);
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
}
