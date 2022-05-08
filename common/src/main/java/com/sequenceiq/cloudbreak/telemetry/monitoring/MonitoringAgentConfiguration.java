package com.sequenceiq.cloudbreak.telemetry.monitoring;

public class MonitoringAgentConfiguration {

    private String user;

    private Integer port;

    private String maxDiskUsage;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMaxDiskUsage() {
        return maxDiskUsage;
    }

    public void setMaxDiskUsage(String maxDiskUsage) {
        this.maxDiskUsage = maxDiskUsage;
    }
}
