package com.sequenceiq.environment.environment.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.environment.api.v1.environment.model.request.CredentialAwareEnvRequest;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

public class EnvironmentCreationDto {

    private final String name;

    private final String description;

    private final String cloudPlatform;

    private final String accountId;

    private final String creator;

    private final LocationDto location;

    private final NetworkDto network;

    private final CredentialAwareEnvRequest credential;

    private final Set<String> regions;

    private final Set<String> proxyNames;

    private final boolean createFreeIpa;

    private final AuthenticationDto authentication;

    private final EnvironmentTelemetry telemetry;

    private final Long created;

    private final SecurityAccessDto securityAccess;

    private final String adminGroupName;

    private final ParametersDto parameters;

    private final ExperimentalFeatures experimentalFeatures;

    private final String parentEnvironmentName;
    //CHECKSTYLE:OFF
    public EnvironmentCreationDto(String name, String description, String cloudPlatform, String accountId,
            String creator, LocationDto location, NetworkDto network, CredentialAwareEnvRequest credential,
            Set<String> regions, Set<String> proxyNames, boolean createFreeIpa, AuthenticationDto authentication,
            Long created, EnvironmentTelemetry telemetry, SecurityAccessDto securityAccess, String adminGroupName,
            ParametersDto parameters, ExperimentalFeatures experimentalFeatures, String parentEnvironmentName) {
        //CHECKSTYLE:ON
        this.name = name;
        this.description = description;
        this.cloudPlatform = cloudPlatform;
        this.accountId = accountId;
        this.creator = creator;
        this.location = location;
        this.network = network;
        this.credential = credential;
        this.createFreeIpa = createFreeIpa;
        this.created = created;
        if (CollectionUtils.isEmpty(regions)) {
            this.regions = new HashSet<>();
        } else {
            this.regions = regions;
        }
        if (CollectionUtils.isEmpty(proxyNames)) {
            this.proxyNames = new HashSet<>();
        } else {
            this.proxyNames = proxyNames;
        }
        this.authentication = authentication;
        this.telemetry = telemetry;
        this.securityAccess = securityAccess;
        this.adminGroupName = adminGroupName;
        this.parameters = parameters;
        this.experimentalFeatures = experimentalFeatures != null ? experimentalFeatures : new ExperimentalFeatures();
        this.parentEnvironmentName = parentEnvironmentName;
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

    public Set<String> getProxyNames() {
        return proxyNames;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCreator() {
        return creator;
    }

    public CredentialAwareEnvRequest getCredential() {
        return credential;
    }

    public EnvironmentTelemetry getTelemetry() {
        return telemetry;
    }

    public boolean isCreateFreeIpa() {
        return createFreeIpa;
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

    public String getParentEnvironmentName() {
        return parentEnvironmentName;
    }

    public static final class Builder {
        private String name;

        private String description;

        private String cloudPlatform;

        private String accountId;

        private String creator;

        private LocationDto location;

        private NetworkDto network;

        private CredentialAwareEnvRequest credential;

        private Set<String> regions;

        private Set<String> proxyNames;

        private EnvironmentTelemetry telemetry;

        private boolean createFreeIpa = true;

        private AuthenticationDto authentication;

        private Long created;

        private SecurityAccessDto securityAccess;

        private String adminGroupName;

        private ParametersDto parameters;

        private ExperimentalFeatures experimentalFeatures;

        private String parentEnvironmentName;

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

        public Builder withProxyNames(Set<String> proxyNames) {
            this.proxyNames = proxyNames;
            return this;
        }

        public Builder withCreateFreeIpa(boolean createFreeIpa) {
            this.createFreeIpa = createFreeIpa;
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

        public Builder withParentEnvironmentName(String parentEnvironmentName) {
            this.parentEnvironmentName = parentEnvironmentName;
            return this;
        }

        public EnvironmentCreationDto build() {
            return new EnvironmentCreationDto(name, description, cloudPlatform, accountId, creator,
                    location, network, credential, regions, proxyNames, createFreeIpa, authentication,
                    created, telemetry, securityAccess, adminGroupName, parameters, experimentalFeatures,
                    parentEnvironmentName);
        }
    }
}
