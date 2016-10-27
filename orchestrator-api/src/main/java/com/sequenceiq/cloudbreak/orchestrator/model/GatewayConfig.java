package com.sequenceiq.cloudbreak.orchestrator.model;

public class GatewayConfig {

    private final String publicAddress;
    private final String privateAddress;
    private final String hostname;
    private final String certificateDir;
    private final String serverCert;
    private final String clientCert;
    private final String clientKey;
    private final Integer gatewayPort;
    private final String saltPassword;
    private final String saltBootPassword;
    private final String signatureKey;

    public GatewayConfig(String publicAddress, String privateAddress, Integer gatewayPort, String certificateDir) {
        this(publicAddress, privateAddress, null, gatewayPort, certificateDir, null, null, null, null, null, null);
    }

    public GatewayConfig(String publicAddress, String privateAddress, String hostname,
            Integer gatewayPort, String certificateDir, String serverCert, String clientCert, String clientKey, String saltPassword, String saltBootPassword,
            String signatureKey) {
        this.publicAddress = publicAddress;
        this.privateAddress = privateAddress;
        this.hostname = hostname;
        this.certificateDir = certificateDir;
        this.gatewayPort = gatewayPort;
        this.serverCert = serverCert;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
        this.saltPassword = saltPassword;
        this.saltBootPassword = saltBootPassword;
        this.signatureKey = signatureKey;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public String getPrivateAddress() {
        return privateAddress;
    }

    public String getHostname() {
        return hostname;
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

    public String getSaltPassword() {
        return saltPassword;
    }

    public String getSaltBootPassword() {
        return saltBootPassword;
    }

    public String getSignatureKey() {
        return signatureKey;
    }

    @Override
    public String toString() {
        return "GatewayConfig{"
                + "publicAddress='" + publicAddress + '\''
                + ", privateAddress='" + privateAddress + '\''
                + ", certificateDir='" + certificateDir + '\''
                + '}';
    }
}
