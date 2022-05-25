package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

public class SaltAuth {

    private String password;

    public SaltAuth(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
