package com.sequenceiq.freeipa.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerRole {
    @JsonProperty("server_server")
    private String serverFqdn;

    @JsonProperty("role_servrole")
    private String role;

    @JsonProperty("status")
    private String status;

    public String getServerFqdn() {
        return serverFqdn;
    }

    public void setServerFqdn(String serverFqdn) {
        this.serverFqdn = serverFqdn;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ServerRole{" +
                "serverFqdn='" + serverFqdn + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
