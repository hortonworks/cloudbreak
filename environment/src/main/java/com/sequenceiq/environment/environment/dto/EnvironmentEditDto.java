package com.sequenceiq.environment.environment.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.environment.network.dto.NetworkDto;

public class EnvironmentEditDto {

    private final String description;

    private final Set<String> regions;

    private final String accountId;

    private LocationDto location;

    private NetworkDto network;

    public EnvironmentEditDto(
        String description,
        Set<String> regions,
        String accountId,
        LocationDto location,
        NetworkDto network) {
        this.description = description;
        this.accountId = accountId;
        if (CollectionUtils.isEmpty(regions)) {
            this.regions = new HashSet<>();
        } else {
            this.regions = regions;
        }
        this.network = network;
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getRegions() {
        return regions;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public String getAccountId() {
        return accountId;
    }

    public NetworkDto getNetworkDto() {
        return network;
    }

    public static final class EnvironmentEditDtoBuilder {
        private String description;

        private Set<String> regions;

        private String accountId;

        private LocationDto location;

        private NetworkDto network;

        private EnvironmentEditDtoBuilder() {
        }

        public static EnvironmentEditDtoBuilder anEnvironmentEditDto() {
            return new EnvironmentEditDtoBuilder();
        }

        public EnvironmentEditDtoBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public EnvironmentEditDtoBuilder withRegions(Set<String> regions) {
            this.regions = regions;
            return this;
        }

        public EnvironmentEditDtoBuilder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public EnvironmentEditDtoBuilder withLocation(LocationDto location) {
            this.location = location;
            return this;
        }

        public EnvironmentEditDtoBuilder withNetwork(NetworkDto network) {
            this.network = network;
            return this;
        }

        public EnvironmentEditDto build() {
            return new EnvironmentEditDto(description, regions, accountId, location, network);
        }

    }
}
