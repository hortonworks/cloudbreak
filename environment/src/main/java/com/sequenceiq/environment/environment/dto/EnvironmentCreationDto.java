package com.sequenceiq.environment.environment.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.environment.api.environment.v1.model.request.CredentialAwareEnvRequest;
import com.sequenceiq.environment.network.dto.NetworkDto;

public class EnvironmentCreationDto {

    private final String name;

    private final String description;

    private final String cloudPlatform;

    private final String accountId;

    private final LocationDto location;

    private final NetworkDto network;

    private final CredentialAwareEnvRequest credential;

    private final Set<String> regions;

    private final Set<String> proxyNames;

    public EnvironmentCreationDto(String name, String description, String cloudPlatform, String accountId, LocationDto location, NetworkDto network,
            CredentialAwareEnvRequest credential, Set<String> regions, Set<String> proxyNames) {
        this.name = name;
        this.description = description;
        this.cloudPlatform = cloudPlatform;
        this.accountId = accountId;
        this.location = location;
        this.network = network;
        this.credential = credential;
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

    public CredentialAwareEnvRequest getCredential() {
        return credential;
    }

    public static final class EnvironmentCreationDtoBuilder {
        private String name;

        private String description;

        private String cloudPlatform;

        private String accountId;

        private LocationDto location;

        private NetworkDto network;

        private CredentialAwareEnvRequest credential;

        private Set<String> regions;

        private Set<String> proxyNames;

        private EnvironmentCreationDtoBuilder() {
        }

        public static EnvironmentCreationDtoBuilder anEnvironmentCreationDto() {
            return new EnvironmentCreationDtoBuilder();
        }

        public EnvironmentCreationDtoBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public EnvironmentCreationDtoBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public EnvironmentCreationDtoBuilder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public EnvironmentCreationDtoBuilder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public EnvironmentCreationDtoBuilder withLocation(LocationDto location) {
            this.location = location;
            return this;
        }

        public EnvironmentCreationDtoBuilder withNetwork(NetworkDto network) {
            this.network = network;
            return this;
        }

        public EnvironmentCreationDtoBuilder withCredential(CredentialAwareEnvRequest credential) {
            this.credential = credential;
            return this;
        }

        public EnvironmentCreationDtoBuilder withRegions(Set<String> regions) {
            this.regions = regions;
            return this;
        }

        public EnvironmentCreationDtoBuilder withProxyNames(Set<String> proxyNames) {
            this.proxyNames = proxyNames;
            return this;
        }

        public EnvironmentCreationDto build() {
            return new EnvironmentCreationDto(name, description, cloudPlatform, accountId, location, network, credential, regions, proxyNames);
        }
    }
}
