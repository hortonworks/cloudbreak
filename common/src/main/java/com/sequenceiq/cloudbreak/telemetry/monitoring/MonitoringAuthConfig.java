package com.sequenceiq.cloudbreak.telemetry.monitoring;

public class MonitoringAuthConfig {

    private final String username;

    private final char[] password;

    public MonitoringAuthConfig(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }
}
