package com.sequenceiq.cloudbreak.client;

public class SaltClientConfig {
    private final String saltPassword;
    private final String saltBootPassword;
    private final String signatureKeyPem;

    public SaltClientConfig(String saltPassword, String saltBootPassword, String signatureKeyPem) {
        this.saltPassword = saltPassword;
        this.saltBootPassword = saltBootPassword;
        this.signatureKeyPem = signatureKeyPem;
    }

    public String getSaltPassword() {
        return saltPassword;
    }

    public String getSaltBootPassword() {
        return saltBootPassword;
    }

    public String getSignatureKeyPem() {
        return signatureKeyPem;
    }
}
