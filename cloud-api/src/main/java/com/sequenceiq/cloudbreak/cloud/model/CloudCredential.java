package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

public class CloudCredential extends DynamicModel {

    private String name;
    private String publicKey;
    private String loginUserName;

    public CloudCredential(String name, String publicKey, String loginUserName) {
        this.name = name;
        this.publicKey = publicKey;
        this.loginUserName = loginUserName;
    }

    public CloudCredential(String name, String publicKey, String loginUserName, Map<String, Object> parameters) {
        this(name, publicKey, loginUserName);
        super.putAll(parameters);
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

    public Long getId() {
        return getParameter("id", Long.class);
    }

}
