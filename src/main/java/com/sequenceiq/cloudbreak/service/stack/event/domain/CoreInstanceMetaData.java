package com.sequenceiq.cloudbreak.service.stack.event.domain;

public class CoreInstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicIp;
    private Integer volumeCount;

    public CoreInstanceMetaData(String instanceId, String privateIp, String publicIp, Integer volumeCount) {
        this.instanceId = instanceId;
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.volumeCount = volumeCount;
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

}
