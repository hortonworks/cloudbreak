package com.sequenceiq.cloudbreak.client;

public class IntrospectRequest {

    private String token;

    public IntrospectRequest() {
    }

    public IntrospectRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
