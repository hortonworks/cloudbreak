package com.sequenceiq.cloudbreak.client;

public class SaltClientConfig {
    private final String saltPassword;
    private final String saltBootPassword;
    private final String signatureKey;

    public SaltClientConfig(String saltPassword, String saltBootPassword, String signatureKey) {
        this.saltPassword = saltPassword;
        this.saltBootPassword = saltBootPassword;
        this.signatureKey = signatureKey;
    }

    public String getSaltPassword() {
        return saltPassword;
    }

    public String getSaltBootPassword() {
        return saltBootPassword;
    }

    public String getSignatureKey() {
        return signatureKey;
    }
}
