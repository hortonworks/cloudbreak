package com.sequenceiq.cloudbreak.service.stack.event;

public class CoreInstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicIp;

    public CoreInstanceMetaData(String instanceId, String privateIp, String publicIp) {
        this.instanceId = instanceId;
        this.privateIp = privateIp;
        this.publicIp = publicIp;
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

}
