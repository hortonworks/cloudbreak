package com.sequenceiq.environment.environment.dto;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.proxy.ProxyDetails;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResource;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.domain.CredentialView;
import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

public class EnvironmentDto implements Payload, AccountAwareResource, EnvironmentDetails {

    private Long id;

    private LocationDto location;

    private String name;

    private String description;

    private CredentialView credentialView;

    private Credential credential;

    private String cloudPlatform;

    private Set<Region> regions;

    private EnvironmentTelemetry telemetry;

    private EnvironmentBackup backup;

    private boolean archived;

    private Long deletionTimestamp = -1L;

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

    private String parentEnvironmentCloudPlatform;

    private ProxyConfig proxyConfig;

    private String environmentServiceVersion;

    private EnvironmentDeletionType deletionType;

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

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
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

    @Override
    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Json getRegionsJson() {
        return Json.silent(regions);
    }

    @Override
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

    public EnvironmentBackup getBackup() {
        return backup;
    }

    public void setBackup(EnvironmentBackup backup) {
        this.backup = backup;
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

    @Override
    public NetworkDto getNetwork() {
        return network;
    }

    @Override
    public EnvironmentFeatures getEnvironmentTelemetryFeatures() {
        if (telemetry != null) {
            return telemetry.getFeatures();
        }
        return null;
    }

    public void setNetwork(NetworkDto network) {
        this.network = network;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public String getSecurityAccessType() {
        if (securityAccess == null) {
            return null;
        }
        return securityAccess.getSecurityAccessType();
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

    @Override
    public ParametersDto getParameters() {
        return parameters;
    }

    public void setParameters(ParametersDto parameters) {
        this.parameters = parameters;
    }

    @Override
    public Tunnel getTunnel() {
        return experimentalFeatures == null ? null : experimentalFeatures.getTunnel();
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

    public Map<String, String> getUserDefinedTags() {
        return tags.getUserDefinedTags();
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

    public String getParentEnvironmentCloudPlatform() {
        return parentEnvironmentCloudPlatform;
    }

    public void setParentEnvironmentCloudPlatform(String parentEnvironmentCloudPlatform) {
        this.parentEnvironmentCloudPlatform = parentEnvironmentCloudPlatform;
    }

    @Override
    public ProxyDetails getProxyDetails() {
        ProxyDetails.Builder builder = ProxyDetails.Builder.builder();
        if (proxyConfig != null) {
            builder = builder.withEnabled(true)
                    .withProtocol(proxyConfig.getProtocol())
                    .withAuthentication(StringUtils.isNoneEmpty(proxyConfig.getUserName()));
        }
        return builder.build();
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public String getEnvironmentServiceVersion() {
        return environmentServiceVersion;
    }

    public void setEnvironmentServiceVersion(String environmentServiceVersion) {
        this.environmentServiceVersion = environmentServiceVersion;
    }

    public EnvironmentDeletionType getDeletionType() {
        return deletionType;
    }

    public void setDeletionType(EnvironmentDeletionType deletionType) {
        this.deletionType = deletionType;
    }

    public Map<String, String> mergeTags(CostTagging costTagging) {
        CDPTagMergeRequest mergeRequest = CDPTagMergeRequest.Builder
                .builder()
                .withEnvironmentTags(tags.getUserDefinedTags())
                .withPlatform(cloudPlatform)
                .withRequestTags(tags.getDefaultTags())
                .build();
        return costTagging.mergeTags(mergeRequest);
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

        private EnvironmentBackup backup;

        private boolean archived;

        private Long deletionTimestamp = -1L;

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

        private String parentEnvironmentCloudPlatform;

        private ProxyConfig proxyConfig;

        private String environmentServiceVersion;

        private EnvironmentDeletionType deletionType;

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

        public Builder withBackup(EnvironmentBackup backup) {
            this.backup = backup;
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

        public Builder withParentEnvironmentCloudPlatform(String parentEnvironmentCloudPlatform) {
            this.parentEnvironmentCloudPlatform = parentEnvironmentCloudPlatform;
            return this;
        }

        public Builder withProxyConfig(ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public Builder withEnvironmentServiceVersion(String environmentServiceVersion) {
            this.environmentServiceVersion = environmentServiceVersion;
            return this;
        }

        public Builder withEnvironmentDeletionType(EnvironmentDeletionType deletionType) {
            this.deletionType = deletionType;
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
            environmentDto.setBackup(backup);
            environmentDto.setRegions(regions);
            environmentDto.setArchived(archived);
            environmentDto.setDeletionTimestamp(deletionTimestamp);
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
            environmentDto.setParentEnvironmentCloudPlatform(parentEnvironmentCloudPlatform);
            environmentDto.setProxyConfig(proxyConfig);
            environmentDto.setEnvironmentServiceVersion(environmentServiceVersion);
            environmentDto.setDeletionType(deletionType);
            return environmentDto;
        }
    }
}
