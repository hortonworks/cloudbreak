package com.sequenceiq.it.cloudbreak.actor;

public class CloudbreakUser {

    private String accessKey;

    private String secretKey;

    private String displayName;

    public CloudbreakUser() {
    }

    public CloudbreakUser(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        displayName = "Default User";
    }

    public CloudbreakUser(String accessKey, String secretKey, String displayName) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.displayName = displayName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + displayName + "}";
    }
}
