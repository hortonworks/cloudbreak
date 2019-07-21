package com.sequenceiq.cloudbreak.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccessKeyAuthMethod {

    @JsonProperty("access_key_id")
    private String accessKeyId;

    @JsonProperty("auth_method")
    private String authMethod;

    public AccessKeyAuthMethod(String accessKeyId, String authMethod) {
        this.accessKeyId = accessKeyId;
        this.authMethod = authMethod;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }
}
