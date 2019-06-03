package com.sequenceiq.cloudbreak.auth.altus.model;

public class AltusCredential {

    private final String accessKey;

    private final char[] privateKey;

    public AltusCredential(String accessKey, char[] privateKey) {
        this.accessKey = accessKey;
        this.privateKey = privateKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public char[] getPrivateKey() {
        return privateKey;
    }

    public boolean isEmpty() {
        return accessKey == null || privateKey == null;
    }
}
