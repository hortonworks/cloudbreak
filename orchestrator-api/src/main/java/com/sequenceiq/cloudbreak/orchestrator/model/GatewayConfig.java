package com.sequenceiq.cloudbreak.orchestrator.model;

public class GatewayConfig {

    // Used by cloudbreak to connect the cluster
    private final String connectionAddress;

    private final String publicAddress;

    private final String privateAddress;

    private final String hostname;

    private final String serverCert;

    private final String clientCert;

    private final String clientKey;

    private final Integer gatewayPort;

    private final String saltPassword;

    private final String saltBootPassword;

    private final String signatureKey;

    private final Boolean knoxGatewayEnabled;

    private final boolean primary;

    private final String saltSignPrivateKey;

    private final String saltSignPublicKey;

    public GatewayConfig(String connectionAddress, String publicAddress, String privateAddress,
            Integer gatewayPort, Boolean knoxGatewayEnabled) {
        this(connectionAddress, publicAddress, privateAddress, null, gatewayPort,
                null, null, null, null, null, null, knoxGatewayEnabled, true, null, null);
    }

    public GatewayConfig(String connectionAddress, String publicAddress, String privateAddress, String hostname,
            Integer gatewayPort, String serverCert, String clientCert, String clientKey, String saltPassword, String saltBootPassword,
            String signatureKey, Boolean knoxGatewayEnabled, boolean primary, String saltSignPrivateKey, String saltSignPublicKey) {
        this.connectionAddress = connectionAddress;
        this.publicAddress = publicAddress;
        this.privateAddress = privateAddress;
        this.hostname = hostname;
        this.gatewayPort = gatewayPort;
        this.serverCert = serverCert;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
        this.saltPassword = saltPassword;
        this.saltBootPassword = saltBootPassword;
        this.signatureKey = signatureKey;
        this.knoxGatewayEnabled = knoxGatewayEnabled;
        this.primary = primary;
        this.saltSignPrivateKey = saltSignPrivateKey;
        this.saltSignPublicKey = saltSignPublicKey;
    }

    public String getConnectionAddress() {
        return connectionAddress;
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

    public String getGatewayUrl() {
        return String.format("https://%s:%d", connectionAddress, gatewayPort);
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

    public Boolean getKnoxGatewayEnabled() {
        return knoxGatewayEnabled;
    }

    public boolean isPrimary() {
        return primary;
    }

    public String getSaltSignPrivateKey() {
        return saltSignPrivateKey;
    }

    public String getSaltSignPublicKey() {
        return saltSignPublicKey;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GatewayConfig{");
        sb.append("connectionAddress='").append(connectionAddress).append('\'');
        sb.append(", publicAddress='").append(publicAddress).append('\'');
        sb.append(", privateAddress='").append(privateAddress).append('\'');
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append(", gatewayPort=").append(gatewayPort);
        sb.append(", knoxGatewayEnabled=").append(knoxGatewayEnabled);
        sb.append(", primary=").append(primary);
        sb.append('}');
        return sb.toString();
    }
}
