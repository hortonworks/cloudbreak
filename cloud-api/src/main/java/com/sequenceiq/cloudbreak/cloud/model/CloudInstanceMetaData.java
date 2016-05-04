package com.sequenceiq.cloudbreak.cloud.model;

public class CloudInstanceMetaData {

    public static final CloudInstanceMetaData EMPTY_METADATA = new CloudInstanceMetaData(null, null, null);
    private static final int DEFAULT_SSH_PORT = 22;
    private final String privateIp;
    private final String publicIp;
    private final int sshPort;
    private final String hypervisor;

    public CloudInstanceMetaData(String privateIp, String publicIp) {
        this(privateIp, publicIp, null);
    }

    public CloudInstanceMetaData(String privateIp, String publicIp, String hypervisor) {
        this(privateIp, publicIp, DEFAULT_SSH_PORT, hypervisor);
    }

    public CloudInstanceMetaData(String privateIp, String publicIp, int sshPort, String hypervisor) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.hypervisor = hypervisor;
        this.sshPort = sshPort;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public int getSshPort() {
        return sshPort;
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
