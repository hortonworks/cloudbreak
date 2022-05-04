package com.sequenceiq.cloudbreak.telemetry.monitoring;

public class ExporterConfiguration {

    private String user;

    private Integer port;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
