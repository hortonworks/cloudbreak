package com.sequenceiq.cloudbreak.clusterproxy;

public record TokenCertInfo(String privateKey, String publicKey, String signCert, String base64DerCert) {

    @Override
    public String toString() {
        return String.format("TokenCertInfo {publicKey: %s}", publicKey);
    }
}
