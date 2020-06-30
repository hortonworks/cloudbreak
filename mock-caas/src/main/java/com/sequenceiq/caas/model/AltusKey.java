package com.sequenceiq.caas.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AltusKey {

    private String accessKeyId;

    private String privateKey = "nHkdxgZR0BaNHaSYM3ooS6rIlpV5E+k1CIkr+jFId2g=";

    public AltusKey(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
