package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class CloudCredential extends DynamicModel {

    public static final String SMART_SENSE_ID = "smartSenseId";

    private final Long id;

    private final String name;

    private final String publicKey;

    private final String loginUserName;

    public CloudCredential(Long id, String name, String publicKey, String loginUserName) {
        this(id, name, publicKey, loginUserName, new HashMap<>());
    }

    public CloudCredential(Long id, String name, String publicKey, String loginUserName, Map<String, Object> parameters) {
        super(parameters);
        this.id = id;
        this.name = name;
        this.publicKey = publicKey;
        this.loginUserName = loginUserName;
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
