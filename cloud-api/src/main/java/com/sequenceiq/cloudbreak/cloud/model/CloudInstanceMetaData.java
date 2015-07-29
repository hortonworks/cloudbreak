package com.sequenceiq.cloudbreak.cloud.model;

public class CloudInstanceMetaData {

    private String privateIp;
    private String publicIp;


    public CloudInstanceMetaData(String privateIp, String publicIp) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
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
                ", privateIp='" + privateIp + '\'' +
                ", publicIp='" + publicIp + '\'' +
                '}';
    }
    //END GENERATED CODE
}
