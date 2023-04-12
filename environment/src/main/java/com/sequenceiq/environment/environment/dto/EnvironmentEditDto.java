package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

public class EnvironmentEditDto {

    private final String description;

    private final String accountId;

    private final EnvironmentTelemetry telemetry;

    private final EnvironmentBackup backup;

    private final NetworkDto network;

    private final AuthenticationDto authentication;

    private final SecurityAccessDto securityAccess;

    private final Tunnel tunnel;

    private final IdBrokerMappingSource idBrokerMappingSource;

    private final CloudStorageValidation cloudStorageValidation;

    private final String adminGroupName;

    private final ParametersDto parameters;

    private final ProxyConfig proxyConfig;

    public EnvironmentEditDto(Builder builder) {
        this.description = builder.description;
        this.accountId = builder.accountId;
        this.network = builder.network;
        this.authentication = builder.authentication;
        this.telemetry = builder.telemetry;
        this.backup = builder.backup;
        this.securityAccess = builder.securityAccess;
        this.tunnel = builder.tunnel;
        this.idBrokerMappingSource = builder.idBrokerMappingSource;
        this.cloudStorageValidation = builder.cloudStorageValidation;
        this.adminGroupName = builder.adminGroupName;
        this.parameters = builder.parameters;
        this.proxyConfig = builder.proxyConfig;
    }

    public String getDescription() {
        return description;
    }

    public String getAccountId() {
        return accountId;
    }

    public NetworkDto getNetworkDto() {
        return network;
    }

    public AuthenticationDto getAuthentication() {
        return authentication;
    }

    public EnvironmentTelemetry getTelemetry() {
        return telemetry;
    }

    public EnvironmentBackup getBackup() {
        return backup;
    }

    public SecurityAccessDto getSecurityAccess() {
        return securityAccess;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public IdBrokerMappingSource getIdBrokerMappingSource() {
        return idBrokerMappingSource;
    }

    public CloudStorageValidation getCloudStorageValidation() {
        return cloudStorageValidation;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public ParametersDto getParameters() {
        return parameters;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "EnvironmentEditDto{" +
                "description='" + description + '\'' +
                ", accountId='" + accountId + '\'' +
                ", telemetry=" + telemetry +
                ", backup=" + backup +
                ", network=" + network +
                ", authentication=" + authentication +
                ", securityAccess=" + securityAccess +
                ", tunnel=" + tunnel +
                ", idBrokerMappingSource=" + idBrokerMappingSource +
                ", cloudStorageValidation=" + cloudStorageValidation +
                ", adminGroupName='" + adminGroupName + '\'' +
                ", parameters=" + parameters +
                ", proxyConfig=" + proxyConfig +
                '}';
    }

    public static final class Builder {
        private String description;

        private String accountId;

        private NetworkDto network;

        private AuthenticationDto authentication;

        private EnvironmentTelemetry telemetry;

        private EnvironmentBackup backup;

        private SecurityAccessDto securityAccess;

        private Tunnel tunnel;

        private IdBrokerMappingSource idBrokerMappingSource;

        private CloudStorageValidation cloudStorageValidation;

        private String adminGroupName;

        private ParametersDto parameters;

        private ProxyConfig proxyConfig;

        private Builder() {
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withNetwork(NetworkDto network) {
            this.network = network;
            return this;
        }

        public Builder withAuthentication(AuthenticationDto authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder withTelemetry(EnvironmentTelemetry telemetry) {
            this.telemetry = telemetry;
            return this;
        }

        public Builder withBackup(EnvironmentBackup backup) {
            this.backup = backup;
            return this;
        }

        public Builder withSecurityAccess(SecurityAccessDto securityAccess) {
            this.securityAccess = securityAccess;
            return this;
        }

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
            this.idBrokerMappingSource = idBrokerMappingSource;
            return this;
        }

        public Builder withCloudStorageValidation(CloudStorageValidation cloudStorageValidation) {
            this.cloudStorageValidation = cloudStorageValidation;
            return this;
        }

        public Builder withAdminGroupName(String adminGroupName) {
            this.adminGroupName = adminGroupName;
            return this;
        }

        public Builder withParameters(ParametersDto parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder withProxyConfig(ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public EnvironmentEditDto build() {
            return new EnvironmentEditDto(this);
        }
    }
}
