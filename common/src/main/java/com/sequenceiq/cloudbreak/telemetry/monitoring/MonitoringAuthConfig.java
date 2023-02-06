package com.sequenceiq.cloudbreak.telemetry.monitoring;

public class MonitoringAuthConfig {

    private final String username;

    private final String password;

    public MonitoringAuthConfig(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
