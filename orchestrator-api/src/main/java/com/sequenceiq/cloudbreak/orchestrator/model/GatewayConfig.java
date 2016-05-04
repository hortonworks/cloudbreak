package com.sequenceiq.cloudbreak.orchestrator.model;

public class  GatewayConfig {

    private String publicAddress;
    private String privateAddress;
    private Integer gatewayPort;
    private String certificateDir;

    public GatewayConfig(String publicAddress, String privateAddress, Integer gatewayPort, String certificateDir) {
        this.privateAddress = privateAddress;
        this.publicAddress = publicAddress;
        this.gatewayPort = gatewayPort;
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

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public String getGatewayUrl() {
        return String.format("https://%s:%d", publicAddress, gatewayPort);
    }
}
