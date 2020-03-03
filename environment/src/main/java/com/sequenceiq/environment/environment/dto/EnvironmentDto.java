package com.sequenceiq.environment.environment.dto;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.domain.CredentialView;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

public class EnvironmentDto implements Payload {

    private Long id;

    private LocationDto location;

    private String name;

    private String description;

    private CredentialView credentialView;

    private Credential credential;

    private String cloudPlatform;

    private Set<Region> regions;

    private EnvironmentTelemetry telemetry;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    // TODO: switch to dto
    private Set<ProxyConfig> proxyConfigs = new HashSet<>();

    private NetworkDto network;

    private String accountId;

    private String resourceCrn;

    private EnvironmentStatus status;

    private String statusReason;

    private String creator;

    private FreeIpaCreationDto freeIpaCreation = FreeIpaCreationDto.builder().build();

    private AuthenticationDto authentication;

    private Long created;

    private SecurityAccessDto securityAccess;

    private String adminGroupName;

    private ParametersDto parameters;

    private ExperimentalFeatures experimentalFeatures = new ExperimentalFeatures();

    private EnvironmentTags tags;

    private String parentEnvironmentCrn;

    private String parentEnvironmentName;

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

    public CredentialView getCredentialView() {
        return credentialView;
    }

    public void setCredentialView(CredentialView credentialView) {
        this.credentialView = credentialView;
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

    public Json getRegionsJson() {
        return Json.silent(regions);
    }

    public Set<Region> getRegions() {
        return regions;
    }

    public void setRegions(Set<Region> regions) {
        this.regions = regions;
    }

    public EnvironmentTelemetry getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(EnvironmentTelemetry telemetry) {
        this.telemetry = telemetry;
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

    public EnvironmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnvironmentStatus status) {
        this.status = status;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public FreeIpaCreationDto getFreeIpaCreation() {
        return freeIpaCreation;
    }

    public void setFreeIpaCreation(FreeIpaCreationDto freeIpaCreation) {
        this.freeIpaCreation = freeIpaCreation;
    }

    public AuthenticationDto getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AuthenticationDto authentication) {
        this.authentication = authentication;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public SecurityAccessDto getSecurityAccess() {
        return securityAccess;
    }

    public void setSecurityAccess(SecurityAccessDto securityAccess) {
        this.securityAccess = securityAccess;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

    public ParametersDto getParameters() {
        return parameters;
    }

    public void setParameters(ParametersDto parameters) {
        this.parameters = parameters;
    }

    public ExperimentalFeatures getExperimentalFeatures() {
        return experimentalFeatures;
    }

    public void setExperimentalFeatures(ExperimentalFeatures experimentalFeatures) {
        this.experimentalFeatures = experimentalFeatures;
    }

    public EnvironmentTags getTags() {
        return tags;
    }

    public void setTags(EnvironmentTags tags) {
        this.tags = tags;
    }

    public String getParentEnvironmentCrn() {
        return parentEnvironmentCrn;
    }

    public void setParentEnvironmentCrn(String parentEnvironmentCrn) {
        this.parentEnvironmentCrn = parentEnvironmentCrn;
    }

    public String getParentEnvironmentName() {
        return parentEnvironmentName;
    }

    public void setParentEnvironmentName(String parentEnvironmentName) {
        this.parentEnvironmentName = parentEnvironmentName;
    }

    @Override
    public String toString() {
        return "EnvironmentDto{"
                + "name='" + name + '\''
                + ", cloudPlatform='" + cloudPlatform + '\''
                + ", resourceCrn='" + resourceCrn + '\''
                + ", status=" + status
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;

        private LocationDto locationDto;

        private String name;

        private String description;

        private CredentialView credentialView;

        private Credential credential;

        private String cloudPlatform;

        private Set<Region> regions;

        private EnvironmentTelemetry telemetry;

        private boolean archived;

        private Long deletionTimestamp = -1L;

        private Set<ProxyConfig> proxyConfigs = new HashSet<>();

        private NetworkDto network;

        private String accountId;

        private String resourceCrn;

        private EnvironmentStatus environmentStatus;

        private String creator;

        private String statusReason;

        private FreeIpaCreationDto freeIpaCreation = FreeIpaCreationDto.builder().build();

        private AuthenticationDto authentication;

        private Long created;

        private SecurityAccessDto securityAccess;

        private String adminGroupName;

        private ParametersDto parameters;

        private ExperimentalFeatures experimentalFeatures;

        private EnvironmentTags tags;

        private String parentEnvironmentCrn;

        private String parentEnvironmentName;

        private Builder() {
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withLocationDto(LocationDto locationDto) {
            this.locationDto = locationDto;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withCredential(Credential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withCredentialView(CredentialView credential) {
            this.credentialView = credential;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withRegions(Set<Region> regions) {
            this.regions = regions;
            return this;
        }

        public Builder withArchived(boolean archived) {
            this.archived = archived;
            return this;
        }

        public Builder withDeletionTimestamp(Long deletionTimestamp) {
            this.deletionTimestamp = deletionTimestamp;
            return this;
        }

        public Builder withProxyConfigs(Set<ProxyConfig> proxyConfigs) {
            this.proxyConfigs = proxyConfigs;
            return this;
        }

        public Builder withNetwork(NetworkDto network) {
            this.network = network;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return this;
        }

        public Builder withCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder withFreeIpaCreation(FreeIpaCreationDto freeIpaCreation) {
            this.freeIpaCreation = freeIpaCreation;
            return this;
        }

        public Builder withTelemetry(EnvironmentTelemetry telemetry) {
            this.telemetry = telemetry;
            return this;
        }

        public Builder withAuthentication(AuthenticationDto authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder withStatusReason(String statusReason) {
            this.statusReason = statusReason;
            return this;
        }

        public Builder withCreated(Long created) {
            this.created = created;
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

        public Builder withTags(EnvironmentTags tags) {
            this.tags = tags;
            return this;
        }

        public Builder withParentEnvironmentCrn(String parentEnvironmentCrn) {
            this.parentEnvironmentCrn = parentEnvironmentCrn;
            return this;
        }

        public Builder withParentEnvironmentName(String parentEnvironmentName) {
            this.parentEnvironmentName = parentEnvironmentName;
            return this;
        }

        public EnvironmentDto build() {
            EnvironmentDto environmentDto = new EnvironmentDto();
            environmentDto.setId(id);
            environmentDto.setLocation(locationDto);
            environmentDto.setName(name);
            environmentDto.setDescription(description);
            environmentDto.setCredential(credential);
            environmentDto.setCredentialView(credentialView);
            environmentDto.setCloudPlatform(cloudPlatform);
            environmentDto.setTelemetry(telemetry);
            environmentDto.setRegions(regions);
            environmentDto.setArchived(archived);
            environmentDto.setDeletionTimestamp(deletionTimestamp);
            environmentDto.setProxyConfigs(proxyConfigs);
            environmentDto.setNetwork(network);
            environmentDto.setAccountId(accountId);
            environmentDto.setResourceCrn(resourceCrn);
            environmentDto.setStatus(environmentStatus);
            environmentDto.setCreator(creator);
            environmentDto.setFreeIpaCreation(freeIpaCreation);
            environmentDto.setAuthentication(authentication);
            environmentDto.setStatusReason(statusReason);
            environmentDto.setCreated(created);
            environmentDto.setSecurityAccess(securityAccess);
            environmentDto.setAdminGroupName(adminGroupName);
            environmentDto.setParameters(parameters);
            environmentDto.setExperimentalFeatures(experimentalFeatures);
            environmentDto.setTags(tags);
            environmentDto.setParentEnvironmentCrn(parentEnvironmentCrn);
            environmentDto.setParentEnvironmentName(parentEnvironmentName);
            return environmentDto;
        }
    }
}
