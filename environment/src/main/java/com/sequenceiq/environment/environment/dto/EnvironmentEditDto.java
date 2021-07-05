package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

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

    public EnvironmentEditDto(
            String description,
            String accountId,
            NetworkDto network,
            AuthenticationDto authentication,
            EnvironmentTelemetry telemetry,
            EnvironmentBackup backup,
            SecurityAccessDto securityAccess,
            Tunnel tunnel,
            IdBrokerMappingSource idBrokerMappingSource,
            CloudStorageValidation cloudStorageValidation,
            String adminGroupName,
            ParametersDto parameters) {
        this.description = description;
        this.accountId = accountId;
        this.network = network;
        this.authentication = authentication;
        this.telemetry = telemetry;
        this.backup = backup;
        this.securityAccess = securityAccess;
        this.tunnel = tunnel;
        this.idBrokerMappingSource = idBrokerMappingSource;
        this.cloudStorageValidation = cloudStorageValidation;
        this.adminGroupName = adminGroupName;
        this.parameters = parameters;
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

    public static EnvironmentEditDtoBuilder builder() {
        return new EnvironmentEditDtoBuilder();
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
                '}';
    }

    public static final class EnvironmentEditDtoBuilder {
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

        private EnvironmentEditDtoBuilder() {
        }

        public EnvironmentEditDtoBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public EnvironmentEditDtoBuilder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public EnvironmentEditDtoBuilder withNetwork(NetworkDto network) {
            this.network = network;
            return this;
        }

        public EnvironmentEditDtoBuilder withAuthentication(AuthenticationDto authentication) {
            this.authentication = authentication;
            return this;
        }

        public EnvironmentEditDtoBuilder withTelemetry(EnvironmentTelemetry telemetry) {
            this.telemetry = telemetry;
            return this;
        }

        public EnvironmentEditDtoBuilder withBackup(EnvironmentBackup backup) {
            this.backup = backup;
            return this;
        }

        public EnvironmentEditDtoBuilder withSecurityAccess(SecurityAccessDto securityAccess) {
            this.securityAccess = securityAccess;
            return this;
        }

        public EnvironmentEditDtoBuilder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public EnvironmentEditDtoBuilder withIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
            this.idBrokerMappingSource = idBrokerMappingSource;
            return this;
        }

        public EnvironmentEditDtoBuilder withCloudStorageValidation(CloudStorageValidation cloudStorageValidation) {
            this.cloudStorageValidation = cloudStorageValidation;
            return this;
        }

        public EnvironmentEditDtoBuilder withAdminGroupName(String adminGroupName) {
            this.adminGroupName = adminGroupName;
            return this;
        }

        public EnvironmentEditDtoBuilder withParameters(ParametersDto parameters) {
            this.parameters = parameters;
            return this;
        }

        public EnvironmentEditDto build() {
            return new EnvironmentEditDto(description, accountId, network, authentication, telemetry, backup, securityAccess, tunnel, idBrokerMappingSource,
                    cloudStorageValidation, adminGroupName, parameters);
        }
    }
}
