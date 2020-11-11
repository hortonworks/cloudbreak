package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class InstanceAuthentication extends DynamicModel {

    private final String publicKey;

    private final String publicKeyId;

    private final String loginUserName;

    @JsonCreator
    public InstanceAuthentication(@JsonProperty("publicKey") String publicKey,
            @JsonProperty("publicKeyId") String publicKeyId,
            @JsonProperty("loginUserName") String loginUserName) {
        this.publicKey = publicKey;
        this.publicKeyId = publicKeyId;
        this.loginUserName = loginUserName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    @Override
    public String toString() {
        return "InstanceAuthentication{" +
                "publicKey='" + publicKey + '\'' +
                ", publicKeyId='" + publicKeyId + '\'' +
                ", loginUserName='" + loginUserName + '\'' +
                '}';
    }
}
