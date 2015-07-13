package com.sequenceiq.cloudbreak.orchestrator;

public class GatewayConfig {

    private String publicAddress;
    private String privateAddress;
    private String certificateDir;

    public GatewayConfig(String publicAddress, String privateAddress, String certificateDir) {
        this.privateAddress = privateAddress;
        this.publicAddress = publicAddress;
        this.certificateDir = certificateDir;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public String getPrivateAddress() {
        return privateAddress;
    }

    public String getCertificateDir() {
        return certificateDir;
    }
}
