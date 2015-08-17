package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

public class CloudCredential extends DynamicModel {

    private String name;
    private String publicKey;

    public CloudCredential(String name, String publicKey) {
        this.name = name;
        this.publicKey = publicKey;
    }

    public CloudCredential(String name, String publicKey, Map<String, Object> parameters) {
        this(name, publicKey);
        super.putAll(parameters);
    }

    public String getName() {
        return name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public Long getId() {
        return getParameter("id", Long.class);
    }

}
