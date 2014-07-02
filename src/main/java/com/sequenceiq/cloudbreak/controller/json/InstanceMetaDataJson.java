package com.sequenceiq.cloudbreak.controller.json;

public class InstanceMetaDataJson implements JsonEntity {

    private String privateIp;
    private String publicIp;

    public InstanceMetaDataJson() {

    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }
}
