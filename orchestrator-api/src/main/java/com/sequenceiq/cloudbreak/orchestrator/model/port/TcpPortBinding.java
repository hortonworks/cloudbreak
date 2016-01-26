package com.sequenceiq.cloudbreak.orchestrator.model.port;

public class TcpPortBinding {

    private Integer exposedPort;
    private String hostIp;
    private Integer hostPort;

    public TcpPortBinding(Integer exposedPort, String hostIp, Integer hostPort) {
        this.exposedPort = exposedPort;
        this.hostIp = hostIp;
        this.hostPort = hostPort;
    }

    public Integer getExposedPort() {
        return exposedPort;
    }

    public String getHostIp() {
        return hostIp;
    }

    public Integer getHostPort() {
        return hostPort;
    }
}
