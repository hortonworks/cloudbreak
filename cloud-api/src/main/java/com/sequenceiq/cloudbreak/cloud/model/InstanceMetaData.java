package com.sequenceiq.cloudbreak.cloud.model;

public class InstanceMetaData {

    private String instanceId;
    private String privateIp;
    private String publicIp;


    public InstanceMetaData(String instanceId, String privateIp, String publicIp) {
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

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "InstanceMetaData{" +
                "instanceId='" + instanceId + '\'' +
                ", privateIp='" + privateIp + '\'' +
                ", publicIp='" + publicIp + '\'' +
                '}';
    }
    //END GENERATED CODE
}
