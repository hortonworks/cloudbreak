package com.sequenceiq.cloudbreak.cloud.model;

public class DatabaseConnectionProperties {

    private String connectionUrl;

    private String username;

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "DatabaseConnectionProperties{" +
                "connectionUrl='" + connectionUrl + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}