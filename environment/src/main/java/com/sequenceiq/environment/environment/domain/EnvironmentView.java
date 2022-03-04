package com.sequenceiq.environment.environment.domain;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.environment.credential.domain.CredentialView;
import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.parameters.dao.converter.EnvironmentDeletionTypeConverter;
import com.sequenceiq.environment.parameters.dao.converter.EnvironmentStatusConverter;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.proxy.domain.ProxyConfigView;

@Entity
@Table(name = "Environment")
public class EnvironmentView extends CompactView implements AuthResource {

    @Column(nullable = false)
    private String cloudPlatform;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private Json regions;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json telemetry;

    @ManyToOne
    @JoinColumn(nullable = false)
    private CredentialView credential;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String locationDisplayName;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    @Column(columnDefinition = "boolean default false")
    private boolean archived;

    @OneToOne(mappedBy = "environment", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private BaseNetwork network;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    @Convert(converter = EnvironmentStatusConverter.class)
    private EnvironmentStatus status;

    @Convert(converter = EnvironmentDeletionTypeConverter.class)
    private EnvironmentDeletionType deletionType;

    private Long deletionTimestamp = -1L;

    @Column(nullable = false)
    private String creator;

    private Long created;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String statusReason;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json backup;

    @Convert(converter = JsonToString.class)
    @Column(name = "experimentalfeatures", columnDefinition = "TEXT")
    private Json experimentalFeaturesJson;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    @Column(name = "admin_group_name")
    private String adminGroupName;

    @Column(name = "environment_service_version")
    private String environmentServiceVersion;

    @Column(name = "environment_domain")
    private String domain;

    @OneToOne(mappedBy = "environment", cascade = CascadeType.ALL, orphanRemoval = true)
    private BaseParameters parameters;

    @Column(nullable = false)
    private boolean createFreeIpa;

    private Integer freeIpaInstanceCountByGroup;

    private String freeIpaImageCatalog;

    private String freeIpaImageId;

    @Column(nullable = false)
    private boolean freeIpaEnableMultiAz;

    @JoinColumn(name = "environment_authentication_id")
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private EnvironmentAuthentication authentication;

    @Column(length = 4000, name = "securitygroup_id_knox")
    private String securityGroupIdForKnox;

    @Column(length = 4000, name = "securitygroup_id_default")
    private String defaultSecurityGroupId;

    private String cidr;

    @ManyToOne
    private ProxyConfigView proxyConfig;

    @ManyToOne
    @JoinColumn(name = "parent_environment_id", referencedColumnName = "id")
    private ParentEnvironmentView parentEnvironment;

    public ProxyConfigView getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfigView proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public ParentEnvironmentView getParentEnvironment() {
        return parentEnvironment;
    }

    public void setParentEnvironment(ParentEnvironmentView parentEnvironment) {
        this.parentEnvironment = parentEnvironment;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public EnvironmentAuthentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(EnvironmentAuthentication authentication) {
        this.authentication = authentication;
    }

    public String getSecurityGroupIdForKnox() {
        return securityGroupIdForKnox;
    }

    public void setSecurityGroupIdForKnox(String securityGroupIdForKnox) {
        this.securityGroupIdForKnox = securityGroupIdForKnox;
    }

    public String getDefaultSecurityGroupId() {
        return defaultSecurityGroupId;
    }

    public void setDefaultSecurityGroupId(String defaultSecurityGroupId) {
        this.defaultSecurityGroupId = defaultSecurityGroupId;
    }

    public Json getRegions() {
        return regions;
    }

    public void setRegions(Json regions) {
        this.regions = regions;
    }

    public Set<Region> getRegionSet() {
        return JsonUtil.jsonToType(regions.getValue(), new RegionSetTypeReference());
    }

    public EnvironmentTelemetry getTelemetry() {
        if (telemetry != null && telemetry.getValue() != null) {
            return JsonUtil.readValueOpt(telemetry.getValue(), EnvironmentTelemetry.class).orElse(null);
        }
        return null;
    }

    public void setTelemetry(EnvironmentTelemetry telemetry) {
        if (telemetry != null) {
            this.telemetry = new Json(telemetry);
        }
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public CredentialView getCredential() {
        return credential;
    }

    public void setCredentialView(CredentialView credential) {
        this.credential = credential;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocationDisplayName() {
        return locationDisplayName;
    }

    public void setLocationDisplayName(String locationDisplayName) {
        this.locationDisplayName = locationDisplayName;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public BaseNetwork getNetwork() {
        return network;
    }

    public void setNetwork(BaseNetwork network) {
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

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public EnvironmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnvironmentStatus status) {
        this.status = status;
    }

    public EnvironmentDeletionType getDeletionType() {
        return deletionType;
    }

    public void setDeletionType(EnvironmentDeletionType deletionType) {
        this.deletionType = deletionType;
    }

    public EnvironmentBackup getBackup() {
        if (backup != null && backup.getValue() != null) {
            return JsonUtil.readValueOpt(backup.getValue(), EnvironmentBackup.class).orElse(null);
        }
        return null;
    }

    public void setBackup(EnvironmentBackup backup) {
        if (backup != null) {
            this.backup = new Json(backup);
        }
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setDeletionTimestamp(Long deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public ExperimentalFeatures getExperimentalFeaturesJson() {
        if (experimentalFeaturesJson != null && experimentalFeaturesJson.getValue() != null) {
            return JsonUtil.readValueOpt(experimentalFeaturesJson.getValue(), ExperimentalFeatures.class).orElse(new ExperimentalFeatures());
        }
        return new ExperimentalFeatures();
    }

    public void setExperimentalFeaturesJson(ExperimentalFeatures experimentalFeaturesJson) {
        if (experimentalFeaturesJson != null) {
            this.experimentalFeaturesJson = new Json(experimentalFeaturesJson);
        }
    }

    public Json getTags() {
        return tags;
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public EnvironmentTags getEnvironmentTags() {
        if (tags != null && tags.getValue() != null) {
            return JsonUtil.readValueOpt(tags.getValue(), EnvironmentTags.class)
                    .orElse(new EnvironmentTags(new HashMap<>(), new HashMap<>()));
        }
        return new EnvironmentTags(new HashMap<>(), new HashMap<>());
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

    public String getEnvironmentServiceVersion() {
        return environmentServiceVersion;
    }

    public void setEnvironmentServiceVersion(String environmentServiceVersion) {
        this.environmentServiceVersion = environmentServiceVersion;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public BaseParameters getParameters() {
        return parameters;
    }

    public void setParameters(BaseParameters parameters) {
        this.parameters = parameters;
    }

    public boolean isCreateFreeIpa() {
        return createFreeIpa;
    }

    public void setCreateFreeIpa(boolean createFreeIpa) {
        this.createFreeIpa = createFreeIpa;
    }

    public Integer getFreeIpaInstanceCountByGroup() {
        return freeIpaInstanceCountByGroup;
    }

    public void setFreeIpaInstanceCountByGroup(Integer freeIpaInstanceCountByGroup) {
        this.freeIpaInstanceCountByGroup = freeIpaInstanceCountByGroup;
    }

    public String getFreeIpaImageCatalog() {
        return freeIpaImageCatalog;
    }

    public void setFreeIpaImageCatalog(String freeIpaImageCatalog) {
        this.freeIpaImageCatalog = freeIpaImageCatalog;
    }

    public String getFreeIpaImageId() {
        return freeIpaImageId;
    }

    public void setFreeIpaImageId(String freeIpaImageId) {
        this.freeIpaImageId = freeIpaImageId;
    }

    public boolean isFreeIpaEnableMultiAz() {
        return freeIpaEnableMultiAz;
    }

    public void setFreeIpaEnableMultiAz(boolean freeIpaEnableMultiAz) {
        this.freeIpaEnableMultiAz = freeIpaEnableMultiAz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnvironmentView that = (EnvironmentView) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "EnvironmentView{" +
                "cloudPlatform='" + cloudPlatform + '\'' +
                ", regions=" + regions +
                ", telemetry=" + telemetry +
                ", credential=" + credential +
                ", location='" + location + '\'' +
                ", locationDisplayName='" + locationDisplayName + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", archived=" + archived +
                ", network=" + network +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", status=" + status +
                ", deletionType=" + deletionType +
                '}';
    }
}
