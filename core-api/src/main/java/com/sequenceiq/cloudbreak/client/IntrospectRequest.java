package com.sequenceiq.cloudbreak.client;

public class IntrospectRequest {

    private final String token;

    public IntrospectRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
