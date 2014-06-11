package com.sequenceiq.cloudbreak.service.azure;

public class Port {
    private String name;
    private String port;
    private String localPort;
    private String protocol;

    public Port() {

    }

    public Port(String name, String localPort, String port, String protocol) {
        this.name = name;
        this.localPort = localPort;
        this.port = port;
        this.protocol = protocol;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getLocalPort() {
        return localPort;
    }

    public void setLocalPort(String localPort) {
        this.localPort = localPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
