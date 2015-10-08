package com.sequenceiq.cloudbreak.service.stack.flow;

public class CoreInstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicIp;
    private Integer volumeCount;
    private Integer containerCount = 0;
    private Long privateId;
    private String instanceGroupName;

    public CoreInstanceMetaData(String instanceId, Long privateId, String privateIp, String publicIp, Integer volumeCount, String instanceGroupName) {
        this.instanceId = instanceId;
        this.privateId = privateId;
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

    public Long getPrivateId() {
        return privateId;
    }
}
