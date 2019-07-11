package com.sequenceiq.it.cloudbreak.actor;

public class CloudbreakUser {

    private final String accessKey;

    private final String secretKey;

    public CloudbreakUser(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }
}
