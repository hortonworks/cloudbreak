package com.sequenceiq.environment.environment.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.store.EnvironmentStatusUpdater;

@Entity
@Table
public class Environment implements AuthResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "environment_generator")
    @SequenceGenerator(name = "environment_generator", sequenceName = "environment_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Credential credential;

    @Column(nullable = false)
    private String cloudPlatform;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private Json regions;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json telemetry;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private boolean createFreeIpa;

    private Integer freeIpaInstanceCountByGroup;

    @Column(nullable = false)
    private String locationDisplayName;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    private boolean archived;

    @Column(nullable = false)
    private String creator;

    private Long deletionTimestamp = -1L;

    @OneToOne(mappedBy = "environment", cascade = CascadeType.ALL, orphanRemoval = true)
    private BaseNetwork network;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    @Enumerated(EnumType.STRING)
    private EnvironmentStatus status;

    @JoinColumn(name = "environment_authentication_id")
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private EnvironmentAuthentication authentication;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String statusReason;

    private Long created;

    @Column(name = "securitygroup_id_knox")
    private String securityGroupIdForKnox;

    @Column(name = "securitygroup_id_default")
    private String defaultSecurityGroupId;

    private String cidr;

    @Column(name = "admin_group_name")
    private String adminGroupName;

    @OneToOne(mappedBy = "environment", cascade = CascadeType.ALL, orphanRemoval = true)
    private BaseParameters parameters;

    @Convert(converter = JsonToString.class)
    @Column(name = "experimentalfeatures", columnDefinition = "TEXT")
    private Json experimentalFeaturesJson;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    @ManyToOne
    @JoinColumn(name = "parent_environment_id", referencedColumnName = "id")
    private Environment parentEnvironment;

    @ManyToOne
    private ProxyConfig proxyConfig;

    public Environment() {
        regions = new Json(new HashSet<Region>());
        tags = new Json(new EnvironmentTags(new HashMap<>(), new HashMap<>()));
        experimentalFeaturesJson = new Json(new ExperimentalFeatures());
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    public void setArchived(boolean archived) {
        this.archived = archived;
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

    public Json getRegions() {
        return regions;
    }

    public void setRegions(Set<Region> regions) {
        this.regions = new Json(regions);
    }

    public Set<Region> getRegionSet() {
        return JsonUtil.jsonToType(regions.getValue(), new RegionSetTypeReference());
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getLocation() {
        return location;
    }

    public String getLocationDisplayName() {
        return locationDisplayName;
    }

    public void setLocationDisplayName(String locationDisplayName) {
        this.locationDisplayName = locationDisplayName;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public boolean isArchived() {
        return archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setDeletionTimestamp(Long deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

    public BaseNetwork getNetwork() {
        return network;
    }

    public void setNetwork(BaseNetwork network) {
        this.network = network;
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

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public void setRegions(Json regions) {
        this.regions = regions;
    }

    public EnvironmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnvironmentStatus status) {
        this.status = status;
        if (id != null) {
            EnvironmentStatusUpdater.update(id, status);
        }
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
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

    public EnvironmentAuthentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(EnvironmentAuthentication authentication) {
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

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
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

    public void setTelemetry(Json telemetry) {
        this.telemetry = telemetry;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

    public BaseParameters getParameters() {
        return parameters;
    }

    public void setParameters(BaseParameters parameters) {
        this.parameters = parameters;
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

    public Environment getParentEnvironment() {
        return parentEnvironment;
    }

    public void setParentEnvironment(Environment parentEnvironment) {
        this.parentEnvironment = parentEnvironment;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }
}
