package com.sequenceiq.it.cloudbreak.actor;

public class CloudbreakUser {

    private final String token;

    public CloudbreakUser(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
