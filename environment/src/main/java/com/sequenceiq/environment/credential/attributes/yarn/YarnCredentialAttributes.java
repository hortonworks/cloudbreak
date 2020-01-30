package com.sequenceiq.environment.credential.attributes.yarn;

public class YarnCredentialAttributes {

    private String ambariUser;

    private String endpoint;

    public String getAmbariUser() {
        return ambariUser;
    }

    public void setAmbariUser(String ambariUser) {
        this.ambariUser = ambariUser;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
