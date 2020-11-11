package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudInstanceMetaData {

    public static final CloudInstanceMetaData EMPTY_METADATA = new CloudInstanceMetaData(null, null);

    private static final int DEFAULT_SSH_PORT = 22;

    private final String privateIp;

    private final String publicIp;

    private final int sshPort;

    private final String localityIndicator;

    private final CloudInstanceLifeCycle lifeCycle;

    public CloudInstanceMetaData(String privateIp, String publicIp) {
        this(privateIp, publicIp, (String) null);
    }

    public CloudInstanceMetaData(String privateIp, String publicIp, CloudInstanceLifeCycle lifeCycle) {
        this(privateIp, publicIp, DEFAULT_SSH_PORT, null, lifeCycle);
    }

    public CloudInstanceMetaData(String privateIp, String publicIp, String localityIndicator) {
        this(privateIp, publicIp, DEFAULT_SSH_PORT, localityIndicator);
    }

    public CloudInstanceMetaData(String privateIp, String publicIp, int sshPort, String localityIndicator) {
        this(privateIp, publicIp, sshPort, localityIndicator, CloudInstanceLifeCycle.getDefault());
    }

    @JsonCreator
    public CloudInstanceMetaData(@JsonProperty("privateIp") String privateIp,
            @JsonProperty("publicIp") String publicIp,
            @JsonProperty("sshPort") int sshPort,
            @JsonProperty("localityIndicator") String localityIndicator,
            @JsonProperty("lifeCycle") CloudInstanceLifeCycle lifeCycle) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.localityIndicator = localityIndicator;
        this.sshPort = sshPort;
        this.lifeCycle = lifeCycle;
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

    public CloudInstanceLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "InstanceMetaData{"
                + ", privateIp='" + privateIp + '\''
                + ", publicIp='" + publicIp + '\''
                + ", localityIndicator='" + localityIndicator + '\''
                + ", lifeCycle='" + lifeCycle + '\''
                + '}';
    }
    //END GENERATED CODE
}
