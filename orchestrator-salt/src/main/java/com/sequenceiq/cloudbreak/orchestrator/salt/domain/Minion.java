package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;

public class Minion {

    private String address;

    private List<String> roles;

    private String server;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}
