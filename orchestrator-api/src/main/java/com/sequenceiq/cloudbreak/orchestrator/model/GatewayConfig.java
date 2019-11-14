package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;

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

    private final String instanceId;

    private final String saltPassword;

    private final String saltBootPassword;

    private final String signatureKey;

    private final Boolean knoxGatewayEnabled;

    private final boolean primary;

    private final String saltSignPrivateKey;

    private final String saltSignPublicKey;

    private final String userFacingCert;

    private final String userFacingKey;

    private Optional<String> path = Optional.empty();

    private String protocol = "https";

    public GatewayConfig(String connectionAddress, String publicAddress, String privateAddress,
            Integer gatewayPort, String instanceId, Boolean knoxGatewayEnabled) {
        this(connectionAddress, publicAddress, privateAddress, null, gatewayPort,
                instanceId, null, null, null, null, null, null, knoxGatewayEnabled, true, null, null, null, null);
    }

    public GatewayConfig(String connectionAddress, String publicAddress, String privateAddress, String hostname,
            Integer gatewayPort, String instanceId, String serverCert, String clientCert, String clientKey, String saltPassword, String saltBootPassword,
            String signatureKey, Boolean knoxGatewayEnabled, boolean primary, String saltSignPrivateKey, String saltSignPublicKey,
            String userFacingCert, String userFacingKey) {
        this.connectionAddress = connectionAddress;
        this.publicAddress = publicAddress;
        this.privateAddress = privateAddress;
        this.hostname = hostname;
        this.gatewayPort = gatewayPort;
        this.instanceId = instanceId;
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
        this.userFacingCert = userFacingCert;
        this.userFacingKey = userFacingKey;
    }

    private GatewayConfig(GatewayConfig gatewayConfig, @Nonnull ServiceEndpoint serviceEndpoint) {
        this(serviceEndpoint.getHostEndpoint().getHostAddressString(), gatewayConfig.publicAddress, gatewayConfig.privateAddress,
                gatewayConfig.hostname, Objects.requireNonNull(serviceEndpoint.getPort().orElse(null), "serviceEndpoint port unspecified"),
                gatewayConfig.instanceId,
                gatewayConfig.serverCert, gatewayConfig.clientCert, gatewayConfig.clientKey,
                gatewayConfig.saltPassword, gatewayConfig.saltBootPassword, gatewayConfig.signatureKey,
                gatewayConfig.knoxGatewayEnabled, gatewayConfig.primary,
                gatewayConfig.saltSignPrivateKey, gatewayConfig.saltSignPublicKey,
                gatewayConfig.getUserFacingCert(), gatewayConfig.getUserFacingKey());
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

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public String getGatewayUrl() {
        return String.format("%s://%s:%d%s", protocol, connectionAddress, gatewayPort, path.orElse(""));
    }

    public String getInstanceId() {
        return instanceId;
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

    public String getUserFacingCert() {
        return userFacingCert;
    }

    public String getUserFacingKey() {
        return userFacingKey;
    }

    public GatewayConfig withPath(String path) {
        this.path = Optional.of(path);
        return this;
    }

    public GatewayConfig withProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("GatewayConfig{");
        sb.append("connectionAddress='").append(connectionAddress).append('\'');
        sb.append(", publicAddress='").append(publicAddress).append('\'');
        sb.append(", privateAddress='").append(privateAddress).append('\'');
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append(", gatewayPort=").append(gatewayPort);
        sb.append(", instanceId=").append(instanceId);
        sb.append(", knoxGatewayEnabled=").append(knoxGatewayEnabled);
        sb.append(", primary=").append(primary);
        sb.append('}');
        return sb.toString();
    }
}
