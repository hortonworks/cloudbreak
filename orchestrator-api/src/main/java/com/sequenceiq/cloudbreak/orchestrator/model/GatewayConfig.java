package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Optional;

import com.sequenceiq.cloudbreak.util.HostUtil;

public final class GatewayConfig {

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

    private final String saltMasterPrivateKey;

    private final String saltMasterPublicKey;

    private final String saltSignPrivateKey;

    private final String saltSignPublicKey;

    private final String userFacingCert;

    private final String userFacingKey;

    private final String alternativeUserFacingCert;

    private final String alternativeUserFacingKey;

    private Optional<String> newServerCert = Optional.empty();

    private Optional<String> path = Optional.empty();

    private String protocol = "https";

    private Optional<String> saltVersion = Optional.empty();

    private GatewayServiceConfig gatewayServiceConfig;

    public GatewayConfig(GatewayConfig.Builder gatewayConfigBuilder) {
        this.connectionAddress = gatewayConfigBuilder.connectionAddress;
        this.publicAddress = gatewayConfigBuilder.publicAddress;
        this.privateAddress = gatewayConfigBuilder.privateAddress;
        this.hostname = gatewayConfigBuilder.hostname;
        this.gatewayPort = gatewayConfigBuilder.gatewayPort;
        this.instanceId = gatewayConfigBuilder.instanceId;
        this.serverCert = gatewayConfigBuilder.serverCert;
        this.clientCert = gatewayConfigBuilder.clientCert;
        this.clientKey = gatewayConfigBuilder.clientKey;
        this.saltPassword = gatewayConfigBuilder.saltPassword;
        this.saltBootPassword = gatewayConfigBuilder.saltBootPassword;
        this.signatureKey = gatewayConfigBuilder.signatureKey;
        this.knoxGatewayEnabled = gatewayConfigBuilder.knoxGatewayEnabled;
        this.primary = gatewayConfigBuilder.primary;
        this.saltMasterPrivateKey = gatewayConfigBuilder.saltMasterPrivateKey;
        this.saltMasterPublicKey = gatewayConfigBuilder.saltMasterPublicKey;
        this.saltSignPrivateKey = gatewayConfigBuilder.saltSignPrivateKey;
        this.saltSignPublicKey = gatewayConfigBuilder.saltSignPublicKey;
        this.userFacingCert = gatewayConfigBuilder.userFacingCert;
        this.userFacingKey = gatewayConfigBuilder.userFacingKey;
        this.alternativeUserFacingCert = gatewayConfigBuilder.alternativeUserFacingCert;
        this.alternativeUserFacingKey = gatewayConfigBuilder.alternativeUserFacingKey;
        this.gatewayServiceConfig = gatewayConfigBuilder.gatewayServiceConfig;
    }

    public static Builder builder() {
        return new Builder();
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

    public Optional<String> getNewServerCert() {
        return newServerCert;
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

    public String getSaltMasterPrivateKey() {
        return saltMasterPrivateKey;
    }

    public String getSaltMasterPublicKey() {
        return saltMasterPublicKey;
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

    public String getAlternativeUserFacingCert() {
        return alternativeUserFacingCert;
    }

    public String getAlternativeUserFacingKey() {
        return alternativeUserFacingKey;
    }

    public GatewayServiceConfig getGatewayServiceConfig() {
        return gatewayServiceConfig;
    }

    public GatewayConfig withPath(String path) {
        this.path = Optional.of(path);
        return this;
    }

    public GatewayConfig withProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public void withNewServerCert(String newServerCert) {
        this.newServerCert = Optional.ofNullable(newServerCert);
    }

    public void withSaltVersion(String saltVersion) {
        this.saltVersion = Optional.ofNullable(saltVersion);
    }

    public Optional<String> getSaltVersion() {
        return saltVersion;
    }

    public Builder toBuilder() {
        return  GatewayConfig.builder()
                .withConnectionAddress(this.connectionAddress)
                .withPublicAddress(this.publicAddress)
                .withPrivateAddress(this.privateAddress)
                .withHostname(this.hostname)
                .withGatewayPort(this.gatewayPort)
                .withInstanceId(this.instanceId)
                .withServerCert(this.serverCert)
                .withClientCert(this.clientCert)
                .withClientKey(this.clientKey)
                .withSaltBootPassword(this.saltBootPassword)
                .withSaltPassword(this.saltPassword)
                .withSignatureKey(this.signatureKey)
                .withPath(this.path)
                .withProtocol(this.protocol)
                .withSaltVersion(this.saltVersion.orElse(null))
                .withKnoxGatewayEnabled(this.knoxGatewayEnabled)
                .withPrimary(this.primary)
                .withSaltMasterPrivateKey(this.saltMasterPrivateKey)
                .withSaltMasterPublicKey(this.saltMasterPublicKey)
                .withSaltSignPrivateKey(this.saltSignPrivateKey)
                .withSaltSignPublicKey(this.saltSignPublicKey)
                .withUserFacingCert(this.userFacingCert)
                .withUserFacingKey(this.userFacingKey)
                .withAlternativeUserFacingCert(this.alternativeUserFacingCert)
                .withAlternativeUserFacingKey(this.alternativeUserFacingKey)
                .withGatewayServiceConfig(this.gatewayServiceConfig);
    }

    @Override
    public String toString() {
        return "GatewayConfig{" +
                "connectionAddress='" + connectionAddress + '\'' +
                ", publicAddress='" + publicAddress + '\'' +
                ", privateAddress='" + privateAddress + '\'' +
                ", hostname='" + hostname + '\'' +
                ", gatewayPort=" + gatewayPort +
                ", instanceId='" + instanceId + '\'' +
                ", knoxGatewayEnabled=" + knoxGatewayEnabled +
                ", primary=" + primary +
                ", path=" + path +
                ", protocol='" + protocol + '\'' +
                ", saltVersion=" + saltVersion +
                ", gatewayServiceConfig=" + gatewayServiceConfig +
                '}';
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

        private String saltMasterPrivateKey;

        private String saltMasterPublicKey;

        private String saltSignPrivateKey;

        private String saltSignPublicKey;

        private String userFacingCert;

        private String userFacingKey;

        private String alternativeUserFacingCert;

        private String alternativeUserFacingKey;

        private GatewayServiceConfig gatewayServiceConfig;

        private Optional<String> path = Optional.empty();

        private String protocol = "https";

        private Optional<String> saltVersion = Optional.empty();

        public Builder() {
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

        public Builder withSaltMasterPrivateKey(String saltMasterPrivateKey) {
            this.saltMasterPrivateKey = saltMasterPrivateKey;
            return this;
        }

        public Builder withSaltMasterPublicKey(String saltMasterPublicKey) {
            this.saltMasterPublicKey = saltMasterPublicKey;
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

        public Builder withAlternativeUserFacingCert(String alternativeUserFacingCert) {
            this.alternativeUserFacingCert = alternativeUserFacingCert;
            return this;
        }

        public Builder withAlternativeUserFacingKey(String alternativeUserFacingKey) {
            this.alternativeUserFacingKey = alternativeUserFacingKey;
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

        public Builder withSaltVersion(String saltVersion) {
            this.saltVersion = Optional.ofNullable(saltVersion);
            return this;
        }

        public Builder withGatewayServiceConfig(GatewayServiceConfig gatewayServiceConfig) {
            this.gatewayServiceConfig = gatewayServiceConfig;
            return this;
        }

        public GatewayConfig build() {
            GatewayConfig gatewayConfig = new GatewayConfig(this);
            path.ifPresent(gatewayConfig::withPath);
            gatewayConfig.withProtocol(protocol);
            saltVersion.ifPresent(gatewayConfig::withSaltVersion);
            return gatewayConfig;
        }
    }
}
