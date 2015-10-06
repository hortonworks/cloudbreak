package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

public class CloudCredential extends DynamicModel {

    private final Long id;
    private final String name;
    private final String publicKey;
    private final String loginUserName;

    public CloudCredential(Long id, String name, String publicKey, String loginUserName) {
        this.id = id;
        this.name = name;
        this.publicKey = publicKey;
        this.loginUserName = loginUserName;
    }

    public CloudCredential(Long id, String name, String publicKey, String loginUserName, Map<String, Object> parameters) {
        this(id, name, publicKey, loginUserName);
        super.putAll(parameters);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

}
