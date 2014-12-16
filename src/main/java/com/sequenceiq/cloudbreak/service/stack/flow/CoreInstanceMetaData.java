package com.sequenceiq.cloudbreak.service.stack.flow;

public class CoreInstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicDns;
    private Integer volumeCount;
    private String longName;
    private Integer containerCount = 0;
    private String hostGroup;

    public CoreInstanceMetaData() {
    }

    public CoreInstanceMetaData(String instanceId, String privateIp, String publicDns, Integer volumeCount, String longName, String hostGroup) {
        this.instanceId = instanceId;
        this.privateIp = privateIp;
        this.publicDns = publicDns;
        this.volumeCount = volumeCount;
        this.longName = longName;
        this.hostGroup = hostGroup;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicDns() {
        return publicDns;
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

    public String getHostGroup() {
        return hostGroup;
    }
}
