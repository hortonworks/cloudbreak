package com.sequenceiq.cloudbreak.service.stack.event.domain;

public class CoreInstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicIp;
    private Integer volumeCount;
    private String longName;

    public CoreInstanceMetaData(String instanceId, String privateIp, String publicIp, Integer volumeCount, String longName) {
        this.instanceId = instanceId;
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.volumeCount = volumeCount;
        this.longName = longName;
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
}
