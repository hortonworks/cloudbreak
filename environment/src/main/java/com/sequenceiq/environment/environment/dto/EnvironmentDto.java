package com.sequenceiq.environment.environment.dto;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

public class EnvironmentDto implements Payload {

    private Long id;

    private LocationDto location;

    private String name;

    private String description;

    // TODO: switch to dto
    private Credential credential;

    private String cloudPlatform;

    private Json regions;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    // TODO: switch to dto
    private Set<ProxyConfig> proxyConfigs = new HashSet<>();

    private NetworkDto network;

    private String accountId;

    private String resourceCrn;

    private EnvironmentStatus environmentStatus;

    @Override
    public Long getResourceId() {
        return id;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Json getRegions() {
        return regions;
    }

    public Set<Region> getRegionSet() {
        return JsonUtil.jsonToType(regions.getValue(), new TypeReference<>() {
        });
    }

    public void setRegions(Json regions) {
        this.regions = regions;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setDeletionTimestamp(Long deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

    public Set<ProxyConfig> getProxyConfigs() {
        return proxyConfigs;
    }

    public void setProxyConfigs(Set<ProxyConfig> proxyConfigs) {
        this.proxyConfigs = proxyConfigs;
    }

    public NetworkDto getNetwork() {
        return network;
    }

    public void setNetwork(NetworkDto network) {
        this.network = network;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    public void setEnvironmentStatus(EnvironmentStatus environmentStatus) {
        this.environmentStatus = environmentStatus;
    }

    public static final class EnvironmentDtoBuilder {
        private Long id;

        private LocationDto locationDto;

        private String name;

        private String description;

        private Credential credential;

        private String cloudPlatform;

        private Json regions;

        private boolean archived;

        private Long deletionTimestamp = -1L;

        private Set<ProxyConfig> proxyConfigs = new HashSet<>();

        private NetworkDto network;

        private String accountId;

        private String resourceCrn;

        private EnvironmentStatus environmentStatus;

        private EnvironmentDtoBuilder() {
        }

        public static EnvironmentDtoBuilder anEnvironmentDto() {
            return new EnvironmentDtoBuilder();
        }

        public EnvironmentDtoBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public EnvironmentDtoBuilder withLocationDto(LocationDto locationDto) {
            this.locationDto = locationDto;
            return this;
        }

        public EnvironmentDtoBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public EnvironmentDtoBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public EnvironmentDtoBuilder withCredential(Credential credential) {
            this.credential = credential;
            return this;
        }

        public EnvironmentDtoBuilder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public EnvironmentDtoBuilder withRegions(Json regions) {
            this.regions = regions;
            return this;
        }

        public EnvironmentDtoBuilder withArchived(boolean archived) {
            this.archived = archived;
            return this;
        }

        public EnvironmentDtoBuilder withDeletionTimestamp(Long deletionTimestamp) {
            this.deletionTimestamp = deletionTimestamp;
            return this;
        }

        public EnvironmentDtoBuilder withProxyConfigs(Set<ProxyConfig> proxyConfigs) {
            this.proxyConfigs = proxyConfigs;
            return this;
        }

        public EnvironmentDtoBuilder withNetwork(NetworkDto network) {
            this.network = network;
            return this;
        }

        public EnvironmentDtoBuilder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public EnvironmentDtoBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnvironmentDtoBuilder withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return this;
        }

        public EnvironmentDto build() {
            EnvironmentDto environmentDto = new EnvironmentDto();
            environmentDto.setId(id);
            environmentDto.setLocation(locationDto);
            environmentDto.setName(name);
            environmentDto.setDescription(description);
            environmentDto.setCredential(credential);
            environmentDto.setCloudPlatform(cloudPlatform);
            environmentDto.setRegions(regions);
            environmentDto.setArchived(archived);
            environmentDto.setDeletionTimestamp(deletionTimestamp);
            environmentDto.setProxyConfigs(proxyConfigs);
            environmentDto.setNetwork(network);
            environmentDto.setAccountId(accountId);
            environmentDto.setResourceCrn(resourceCrn);
            environmentDto.setEnvironmentStatus(environmentStatus);
            return environmentDto;
        }
    }
}
