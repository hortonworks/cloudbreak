package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

public class SaltMaster {

    private String address;

    private SaltAuth auth;

    private String domain;

    private String hostName;

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

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
}
