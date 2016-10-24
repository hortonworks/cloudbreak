package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

public class SaltMaster {

    private String address;

    private SaltAuth auth;

    private String domain;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SaltAuth getAuth() {
        return auth;
    }

    public void setAuth(SaltAuth auth) {
        this.auth = auth;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
