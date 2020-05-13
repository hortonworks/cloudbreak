package com.sequenceiq.environment.experience.common;

public class CommonExperience {

    private String name;

    private String internalEnvEndpoint;

    private String hostAddress;

    private String port;

    public CommonExperience(String name, String hostAddress, String internalEnvEndpoint, String port) {
        this.name = name;
        this.hostAddress = hostAddress;
        this.internalEnvEndpoint = internalEnvEndpoint;
        this.port = port;
    }

    public CommonExperience() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInternalEnvEndpoint() {
        return internalEnvEndpoint;
    }

    public void setInternalEnvEndpoint(String internalEnvEndpoint) {
        this.internalEnvEndpoint = internalEnvEndpoint;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

}
