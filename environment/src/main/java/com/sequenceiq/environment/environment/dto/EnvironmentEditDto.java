package com.sequenceiq.environment.environment.dto;

import java.util.Map;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

public class EnvironmentEditDto {

    private final String description;

    private final String accountId;

    private final String crn;

    private final String creator;

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

    private final Map<String, String> userDefinedTags;

    private final String cloudPlatform;

    private final EnvironmentDataServices dataServices;

    private final Integer freeipaNodeCount;

    private final EnvironmentHybridDto environmentHybridDto;

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
        this.userDefinedTags = builder.userDefinedTags;
        this.creator = builder.creator;
        this.crn = builder.crn;
        this.cloudPlatform = builder.cloudPlatform;
        this.dataServices = builder.dataServices;
        this.freeipaNodeCount = builder.freeipaNodeCount;
        this.environmentHybridDto = builder.environmentHybridDto;
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

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    public String getCrn() {
        return crn;
    }

    public String getCreator() {
        return creator;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public EnvironmentDataServices getDataServices() {
        return dataServices;
    }

    public Integer getFreeipaNodeCount() {
        return freeipaNodeCount;
    }

    public EnvironmentHybridDto getEnvironmentHybridDto() {
        return environmentHybridDto;
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
                ", tags=" + userDefinedTags +
                ", creator=" + creator +
                ", crn=" + crn +
                ", cloudPlatform=" + cloudPlatform +
                ", dataServices=" + dataServices +
                ", freeipaNodeCount=" + freeipaNodeCount +
                ", environmentHybridDto=" + environmentHybridDto +
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

        private Map<String, String> userDefinedTags;

        private String crn;

        private String creator;

        private String cloudPlatform;

        private EnvironmentDataServices dataServices;

        private Integer freeipaNodeCount;

        private EnvironmentHybridDto environmentHybridDto;

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

        public Builder withUserDefinedTags(Map<String, String> userDefinedTags) {
            this.userDefinedTags = userDefinedTags;
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

        public Builder withHybridEnvironment(EnvironmentHybridDto environmentHybridDto) {
            this.environmentHybridDto = environmentHybridDto;
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

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder withDataServices(EnvironmentDataServices dataServices) {
            this.dataServices = dataServices;
            return this;
        }

        public Builder withFreeipaNodeCount(Integer freeipaNodeCount) {
            this.freeipaNodeCount = freeipaNodeCount;
            return this;
        }

        public EnvironmentEditDto build() {
            return new EnvironmentEditDto(this);
        }
    }
}
