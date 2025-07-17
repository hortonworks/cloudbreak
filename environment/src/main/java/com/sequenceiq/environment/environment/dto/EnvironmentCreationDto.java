package com.sequenceiq.environment.environment.dto;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.api.v1.environment.model.request.CredentialAwareEnvRequest;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

public class EnvironmentCreationDto {

    private final String name;

    private final String description;

    private final String cloudPlatform;

    private final String accountId;

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    private final String creator;

    private final LocationDto location;

    private final NetworkDto network;

    private final CredentialAwareEnvRequest credential;

    private final Set<String> regions;

    private final ExternalizedComputeClusterDto externalizedComputeCluster;

    private final FreeIpaCreationDto freeIpaCreation;

    private final AuthenticationDto authentication;

    private final EnvironmentTelemetry telemetry;

    private final EnvironmentBackup backup;

    private final Long created;

    private final SecurityAccessDto securityAccess;

    private final String adminGroupName;

    private final ParametersDto parameters;

    private final String crn;

    private final ExperimentalFeatures experimentalFeatures;

    private final Map<String, String> tags;

    private final String parentEnvironmentName;

    private final String proxyConfigName;

    private final String creatorClient;

    private final EnvironmentDataServices dataServices;

    private final EnvironmentType environmentType;

    private final String encryptionProfileName;

    private EnvironmentCreationDto(Builder builder) {
        name = builder.name;
        description = builder.description;
        cloudPlatform = builder.cloudPlatform;
        accountId = builder.accountId;
        creator = builder.creator;
        location = builder.location;
        network = builder.network;
        credential = builder.credential;
        freeIpaCreation = builder.freeIpaCreation;
        externalizedComputeCluster = builder.externalizedComputeCluster;
        created = builder.created;
        if (CollectionUtils.isEmpty(builder.regions)) {
            regions = new HashSet<>();
        } else {
            regions = builder.regions;
        }
        authentication = builder.authentication;
        telemetry = builder.telemetry;
        backup = builder.backup;
        securityAccess = builder.securityAccess;
        adminGroupName = builder.adminGroupName;
        parameters = builder.parameters;
        experimentalFeatures = Objects.requireNonNullElseGet(builder.experimentalFeatures, ExperimentalFeatures::new);
        tags = Objects.requireNonNullElseGet(builder.tags, HashMap::new);
        crn = builder.crn;
        parentEnvironmentName = builder.parentEnvironmentName;
        proxyConfigName = builder.proxyConfigName;
        dataServices = builder.dataServices;
        creatorClient = builder.creatorClient;
        environmentType = builder.environmentType;
        encryptionProfileName = builder.encryptionProfileName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public Set<String> getRegions() {
        return regions;
    }

    public NetworkDto getNetwork() {
        return network;
    }

    public LocationDto getLocation() {
        return location;
    }

    public String getAccountId() {
        return accountId;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public String getCreator() {
        return creator;
    }

    public CredentialAwareEnvRequest getCredential() {
        return credential;
    }

    public EnvironmentTelemetry getTelemetry() {
        return telemetry;
    }

    public EnvironmentBackup getBackup() {
        return backup;
    }

    public FreeIpaCreationDto getFreeIpaCreation() {
        return freeIpaCreation;
    }

    public ExternalizedComputeClusterDto getExternalizedComputeCluster() {
        return externalizedComputeCluster;
    }

    public AuthenticationDto getAuthentication() {
        return authentication;
    }

    public Long getCreated() {
        return created;
    }

    public SecurityAccessDto getSecurityAccess() {
        return securityAccess;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public ParametersDto getParameters() {
        return parameters;
    }

    public ExperimentalFeatures getExperimentalFeatures() {
        return experimentalFeatures;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getCrn() {
        return crn;
    }

    public String getParentEnvironmentName() {
        return parentEnvironmentName;
    }

    public String getProxyConfigName() {
        return proxyConfigName;
    }

    public EnvironmentDataServices getDataServices() {
        return dataServices;
    }

    public String getCreatorClient() {
        return creatorClient;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public String getEncryptionProfileName() {
        return encryptionProfileName;
    }

    @Override
    public String toString() {
        return "EnvironmentCreationDto{" +
                "name='" + name + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", creator='" + creator + '\'' +
                ", crn='" + crn + '\'' +
                ", parentEnvironmentName='" + parentEnvironmentName + '\'' +
                (isNotEmpty(creatorClient) ? ", creatorClient='" + creatorClient + '\'' : "") +
                ", environmentType='" + environmentType + '\'' +
                '}';
    }

    public static final class Builder {
        private String name;

        private String description;

        private String cloudPlatform;

        private String accountId;

        /**
         * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
         * and can become invalid, usage of it can be error prone
         */
        @Deprecated
        private String creator;

        private LocationDto location;

        private NetworkDto network;

        private CredentialAwareEnvRequest credential;

        private Set<String> regions;

        private EnvironmentTelemetry telemetry;

        private EnvironmentBackup backup;

        private FreeIpaCreationDto freeIpaCreation;

        private ExternalizedComputeClusterDto externalizedComputeCluster;

        private AuthenticationDto authentication;

        private Long created;

        private SecurityAccessDto securityAccess;

        private String adminGroupName;

        private ParametersDto parameters;

        private ExperimentalFeatures experimentalFeatures;

        private Map<String, String> tags = new HashMap<>();

        private String crn;

        private String parentEnvironmentName;

        private String proxyConfigName;

        private EnvironmentDataServices dataServices;

        private String creatorClient;

        private EnvironmentType environmentType;

        private String encryptionProfileName;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
         * and can become invalid, usage of it can be error prone
         */
        @Deprecated
        public Builder withCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder withLocation(LocationDto location) {
            this.location = location;
            return this;
        }

        public Builder withNetwork(NetworkDto network) {
            this.network = network;
            return this;
        }

        public Builder withCredential(CredentialAwareEnvRequest credential) {
            this.credential = credential;
            return this;
        }

        public Builder withRegions(Set<String> regions) {
            this.regions = regions;
            return this;
        }

        public Builder withFreeIpaCreation(FreeIpaCreationDto freeIpaCreation) {
            this.freeIpaCreation = freeIpaCreation;
            return this;
        }

        public Builder withExternalizedComputeCluster(ExternalizedComputeClusterDto externalizedComputeCluster) {
            this.externalizedComputeCluster = externalizedComputeCluster;
            return this;
        }

        public Builder withAuthentication(AuthenticationDto authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder withCreated(Long created) {
            this.created = created;
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

        public Builder withAdminGroupName(String adminGroupName) {
            this.adminGroupName = adminGroupName;
            return this;
        }

        public Builder withParameters(ParametersDto parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder withExperimentalFeatures(ExperimentalFeatures experimentalFeatures) {
            this.experimentalFeatures = experimentalFeatures;
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withParentEnvironmentName(String parentEnvironmentName) {
            this.parentEnvironmentName = parentEnvironmentName;
            return this;
        }

        public Builder withProxyConfigName(String proxyConfigName) {
            this.proxyConfigName = proxyConfigName;
            return this;
        }

        public Builder withDataServices(EnvironmentDataServices dataServices) {
            this.dataServices = dataServices;
            return this;
        }

        public Builder withCreatorClient(String creatorClient) {
            this.creatorClient = creatorClient;
            return this;
        }

        public Builder withEnvironmentType(EnvironmentType environmentType) {
            this.environmentType = environmentType;
            return this;
        }

        public Builder withEncryptionProfileName(String encryptionProfileName) {
            this.encryptionProfileName = encryptionProfileName;
            return this;
        }

        public EnvironmentCreationDto build() {
            return new EnvironmentCreationDto(this);
        }
    }
}
