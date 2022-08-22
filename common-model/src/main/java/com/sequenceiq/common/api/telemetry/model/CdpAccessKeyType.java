package com.sequenceiq.common.api.telemetry.model;

public enum CdpAccessKeyType {
    ED25519("Ed25519"), ECDSA("ECDSA"), RSA("RSA");

    private final String value;

    CdpAccessKeyType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
