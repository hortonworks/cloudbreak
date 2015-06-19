package com.sequenceiq.cloudbreak.orchestrator;

public class GatewayConfig {

    private String address;
    private String certificateDir;

    public GatewayConfig(String address, String certificateDir) {
        this.address = address;
        this.certificateDir = certificateDir;
    }

    public String getAddress() {
        return address;
    }

    public String getCertificateDir() {
        return certificateDir;
    }
}
