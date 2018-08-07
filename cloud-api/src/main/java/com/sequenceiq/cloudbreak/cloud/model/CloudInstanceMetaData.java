package com.sequenceiq.cloudbreak.cloud.model;

public class CloudInstanceMetaData {

    public static final CloudInstanceMetaData EMPTY_METADATA = new CloudInstanceMetaData(null, null, null);

    private static final int DEFAULT_SSH_PORT = 22;

    private final String privateIp;

    private final String publicIp;

    private final int sshPort;

    private final String localityIndicator;

    public CloudInstanceMetaData(String privateIp, String publicIp) {
        this(privateIp, publicIp, null);
    }

    public CloudInstanceMetaData(String privateIp, String publicIp, String localityIndicator) {
        this(privateIp, publicIp, DEFAULT_SSH_PORT, localityIndicator);
    }

    public CloudInstanceMetaData(String privateIp, String publicIp, int sshPort, String localityIndicator) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.localityIndicator = localityIndicator;
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

    public String getLocalityIndicator() {
        return localityIndicator;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "InstanceMetaData{"
                + ", privateIp='" + privateIp + '\''
                + ", publicIp='" + publicIp + '\''
                + ", localityIndicator='" + localityIndicator + '\''
                + '}';
    }
    //END GENERATED CODE
}
