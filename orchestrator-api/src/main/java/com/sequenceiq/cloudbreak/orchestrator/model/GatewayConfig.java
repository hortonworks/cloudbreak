package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;
import com.sequenceiq.cloudbreak.util.HostUtil;

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
        if (HostUtil.hasPort(connectionAddress)) {
            return String.format("%s://%s%s", protocol, connectionAddress, path.orElse(""));
        }
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

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this.connectionAddress, this.publicAddress, this.privateAddress, this.hostname, this.serverCert, this.clientCert, this.clientKey,
                this.gatewayPort, this.instanceId, this.saltPassword, this.saltBootPassword, this.signatureKey, this.knoxGatewayEnabled, this.primary,
                this.saltSignPrivateKey, this.saltSignPublicKey, this.userFacingCert, this.userFacingKey, this.path, this.protocol);
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

    public static class Builder {
        private String connectionAddress;

        private String publicAddress;

        private String privateAddress;

        private String hostname;

        private String serverCert;

        private String clientCert;

        private String clientKey;

        private Integer gatewayPort;

        private String instanceId;

        private String saltPassword;

        private String saltBootPassword;

        private String signatureKey;

        private Boolean knoxGatewayEnabled;

        private boolean primary;

        private String saltSignPrivateKey;

        private String saltSignPublicKey;

        private String userFacingCert;

        private String userFacingKey;

        private Optional<String> path = Optional.empty();

        private String protocol = "https";

        public Builder() {
        }

        public Builder(String connectionAddress, String publicAddress, String privateAddress, String hostname, String serverCert, String clientCert,
                String clientKey,
                Integer gatewayPort, String instanceId, String saltPassword, String saltBootPassword, String signatureKey, Boolean knoxGatewayEnabled,
                boolean primary,
                String saltSignPrivateKey, String saltSignPublicKey, String userFacingCert, String userFacingKey, Optional<String> path, String protocol) {
            this.connectionAddress = connectionAddress;
            this.publicAddress = publicAddress;
            this.privateAddress = privateAddress;
            this.hostname = hostname;
            this.serverCert = serverCert;
            this.clientCert = clientCert;
            this.clientKey = clientKey;
            this.gatewayPort = gatewayPort;
            this.instanceId = instanceId;
            this.saltPassword = saltPassword;
            this.saltBootPassword = saltBootPassword;
            this.signatureKey = signatureKey;
            this.knoxGatewayEnabled = knoxGatewayEnabled;
            this.primary = primary;
            this.saltSignPrivateKey = saltSignPrivateKey;
            this.saltSignPublicKey = saltSignPublicKey;
            this.userFacingCert = userFacingCert;
            this.userFacingKey = userFacingKey;
            this.path = path;
            this.protocol = protocol;
        }

        public Builder withConnectionAddress(String connectionAddress) {
            this.connectionAddress = connectionAddress;
            return this;
        }

        public Builder withPublicAddress(String publicAddress) {
            this.publicAddress = publicAddress;
            return this;
        }

        public Builder withPrivateAddress(String privateAddress) {
            this.privateAddress = privateAddress;
            return this;
        }

        public Builder withHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder withServerCert(String serverCert) {
            this.serverCert = serverCert;
            return this;
        }

        public Builder withClientCert(String clientCert) {
            this.clientCert = clientCert;
            return this;
        }

        public Builder withClientKey(String clientKey) {
            this.clientKey = clientKey;
            return this;
        }

        public Builder withGatewayPort(Integer gatewayPort) {
            this.gatewayPort = gatewayPort;
            return this;
        }

        public Builder withInstanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder withSaltPassword(String saltPassword) {
            this.saltPassword = saltPassword;
            return this;
        }

        public Builder withSaltBootPassword(String saltBootPassword) {
            this.saltBootPassword = saltBootPassword;
            return this;
        }

        public Builder withSignatureKey(String signatureKey) {
            this.signatureKey = signatureKey;
            return this;
        }

        public Builder withKnoxGatewayEnabled(Boolean knoxGatewayEnabled) {
            this.knoxGatewayEnabled = knoxGatewayEnabled;
            return this;
        }

        public Builder withPrimary(boolean primary) {
            this.primary = primary;
            return this;
        }

        public Builder withSaltSignPrivateKey(String saltSignPrivateKey) {
            this.saltSignPrivateKey = saltSignPrivateKey;
            return this;
        }

        public Builder withSaltSignPublicKey(String saltSignPublicKey) {
            this.saltSignPublicKey = saltSignPublicKey;
            return this;
        }

        public Builder withUserFacingCert(String userFacingCert) {
            this.userFacingCert = userFacingCert;
            return this;
        }

        public Builder withUserFacingKey(String userFacingKey) {
            this.userFacingKey = userFacingKey;
            return this;
        }

        public Builder withPath(Optional<String> path) {
            this.path = path;
            return this;
        }

        public Builder withProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public GatewayConfig build() {
            GatewayConfig gatewayConfig =
                    new GatewayConfig(connectionAddress, publicAddress, privateAddress, hostname, gatewayPort, instanceId, serverCert, clientCert, clientKey,
                            saltPassword, saltBootPassword, signatureKey, knoxGatewayEnabled, primary, saltSignPrivateKey, saltSignPublicKey, userFacingCert,
                            userFacingKey);
            path.ifPresent(gatewayConfig::withPath);
            gatewayConfig.withProtocol(protocol);
            return gatewayConfig;
        }
    }
}
