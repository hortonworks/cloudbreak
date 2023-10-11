package com.sequenceiq.cloudbreak.service;

public record TokenCertInfo(String privateKey, String publicKey, String base64DerCert) {

    @Override
    public String toString() {
        return String.format("TokenCertInfo {publicKey: %s}", publicKey);
    }
}
