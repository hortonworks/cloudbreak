package com.sequenceiq.cloudbreak.orchestrator.model;

public class GatewayConfig {

    private final String publicAddress;
    private final String privateAddress;
    private final String certificateDir;
    private Integer gatewayPort;
    private final String serverCert;
    private final String clientCert;
    private final String clientKey;

    public GatewayConfig(String publicAddress, String privateAddress, Integer gatewayPort, String certificateDir) {
        this(publicAddress, privateAddress, gatewayPort, certificateDir, null, null, null);
    }

    public GatewayConfig(String publicAddress, String privateAddress, Integer gatewayPort, String certificateDir, String serverCert, String clientCert, String clientKey) {
        this.publicAddress = publicAddress;
        this.privateAddress = privateAddress;
        this.certificateDir = certificateDir;
        this.gatewayPort = gatewayPort;
        this.serverCert = serverCert;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
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

    public String getServerCert() {
        return serverCert;
    }

    public String getClientCert() {
        return clientCert;
    }

    public String getClientKey() {
        return clientKey;
    }
}
