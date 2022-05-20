package com.sequenceiq.environment.environment.dto;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

public class EnvironmentDtoBase implements Payload, AccountAwareResource {

    private Long id;

    private LocationDto location;

    private String name;

    private String originalName;

    private String description;

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

    private String environmentServiceVersion;

    private EnvironmentDeletionType deletionType;

    private String domain;

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

    public NetworkDto getNetwork() {
        return network;
    }

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

    public ParametersDto getParameters() {
        return parameters;
    }

    public void setParameters(ParametersDto parameters) {
        this.parameters = parameters;
    }

    public Tunnel getTunnel() {
        return experimentalFeatures == null ? null : experimentalFeatures.getTunnel();
    }

    public CcmV2TlsType getTlsType() {
        return experimentalFeatures == null ? null : experimentalFeatures.getCcmV2TlsType();
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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    @Override
    public String toString() {
        return "EnvironmentDtoBase{"
                + "name='" + name + '\''
                + "originalName='" + originalName + '\''
                + ", cloudPlatform='" + cloudPlatform + '\''
                + ", resourceCrn='" + resourceCrn + '\''
                + ", status='" + status + '\''
                + ", domain='" + domain + '\''
                + '}';
    }

    public abstract static class EnvironmentDtoBaseBuilder<T extends EnvironmentDtoBase, B extends EnvironmentDtoBaseBuilder<T, B>> {
        private Long id;

        private LocationDto locationDto;

        private String name;

        private String originalName;

        private String description;

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

        private ExperimentalFeatures experimentalFeatures = new ExperimentalFeatures();

        private EnvironmentTags tags;

        private String parentEnvironmentCrn;

        private String parentEnvironmentName;

        private String parentEnvironmentCloudPlatform;

        private String environmentServiceVersion;

        private EnvironmentDeletionType deletionType;

        private String domain;

        protected EnvironmentDtoBaseBuilder() {
        }

        public B withId(Long id) {
            this.id = id;
            return (B) this;
        }

        public B withLocationDto(LocationDto locationDto) {
            this.locationDto = locationDto;
            return (B) this;
        }

        public B withName(String name) {
            this.name = name;
            return (B) this;
        }

        public B withOriginalName(String originalName) {
            this.originalName = originalName;
            return (B) this;
        }

        public B withDescription(String description) {
            this.description = description;
            return (B) this;
        }

        public B withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return (B) this;
        }

        public B withRegions(Set<Region> regions) {
            this.regions = regions;
            return (B) this;
        }

        public B withArchived(boolean archived) {
            this.archived = archived;
            return (B) this;
        }

        public B withDeletionTimestamp(Long deletionTimestamp) {
            this.deletionTimestamp = deletionTimestamp;
            return (B) this;
        }

        public B withNetwork(NetworkDto network) {
            this.network = network;
            return (B) this;
        }

        public B withAccountId(String accountId) {
            this.accountId = accountId;
            return (B) this;
        }

        public B withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return (B) this;
        }

        public B withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return (B) this;
        }

        public B withCreator(String creator) {
            this.creator = creator;
            return (B) this;
        }

        public B withFreeIpaCreation(FreeIpaCreationDto freeIpaCreation) {
            this.freeIpaCreation = freeIpaCreation;
            return (B) this;
        }

        public B withTelemetry(EnvironmentTelemetry telemetry) {
            this.telemetry = telemetry;
            return (B) this;
        }

        public B withBackup(EnvironmentBackup backup) {
            this.backup = backup;
            return (B) this;
        }

        public B withAuthentication(AuthenticationDto authentication) {
            this.authentication = authentication;
            return (B) this;
        }

        public B withStatusReason(String statusReason) {
            this.statusReason = statusReason;
            return (B) this;
        }

        public B withCreated(Long created) {
            this.created = created;
            return (B) this;
        }

        public B withSecurityAccess(SecurityAccessDto securityAccess) {
            this.securityAccess = securityAccess;
            return (B) this;
        }

        public B withAdminGroupName(String adminGroupName) {
            this.adminGroupName = adminGroupName;
            return (B) this;
        }

        public B withParameters(ParametersDto parameters) {
            this.parameters = parameters;
            return (B) this;
        }

        public B withExperimentalFeatures(ExperimentalFeatures experimentalFeatures) {
            this.experimentalFeatures = experimentalFeatures;
            return (B) this;
        }

        public B withTags(EnvironmentTags tags) {
            this.tags = tags;
            return (B) this;
        }

        public B withParentEnvironmentCrn(String parentEnvironmentCrn) {
            this.parentEnvironmentCrn = parentEnvironmentCrn;
            return (B) this;
        }

        public B withParentEnvironmentName(String parentEnvironmentName) {
            this.parentEnvironmentName = parentEnvironmentName;
            return (B) this;
        }

        public B withParentEnvironmentCloudPlatform(String parentEnvironmentCloudPlatform) {
            this.parentEnvironmentCloudPlatform = parentEnvironmentCloudPlatform;
            return (B) this;
        }

        public B withEnvironmentServiceVersion(String environmentServiceVersion) {
            this.environmentServiceVersion = environmentServiceVersion;
            return (B) this;
        }

        public B withEnvironmentDeletionType(EnvironmentDeletionType deletionType) {
            this.deletionType = deletionType;
            return (B) this;
        }

        public B withEnvironmentDomain(String domain) {
            this.domain = domain;
            return (B) this;
        }

        protected void build(T environmentDto) {
            environmentDto.setId(id);
            environmentDto.setLocation(locationDto);
            environmentDto.setName(name);
            environmentDto.setOriginalName(originalName);
            environmentDto.setDescription(description);
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
            environmentDto.setEnvironmentServiceVersion(environmentServiceVersion);
            environmentDto.setDeletionType(deletionType);
            environmentDto.setDomain(domain);
        }

        public abstract T build();
    }
}
