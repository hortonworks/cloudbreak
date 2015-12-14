package com.sequenceiq.cloudbreak.cloud.model;

public class CloudInstanceMetaData {

    private final String privateIp;
    private final String publicIp;
    private final String hypervisor;

    public CloudInstanceMetaData(String privateIp, String publicIp) {
        this(privateIp, publicIp, null);
    }

    public CloudInstanceMetaData(String privateIp, String publicIp, String hypervisor) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.hypervisor = hypervisor;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "InstanceMetaData{" +
                ", privateIp='" + privateIp + '\'' +
                ", publicIp='" + publicIp + '\'' +
                ", hypervisor='" + hypervisor + '\'' +
                '}';
    }
    //END GENERATED CODE
}
