package com.sequenceiq.environment.environment.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.converter.SeLinuxConverter;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.parameters.dao.converter.EnvironmentDeletionTypeConverter;
import com.sequenceiq.environment.parameters.dao.converter.EnvironmentStatusConverter;
import com.sequenceiq.environment.parameters.dao.converter.EnvironmentTypeConverter;
import com.sequenceiq.environment.parameters.dao.converter.FreeIpaLoadBalancerTypeConverter;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.store.EnvironmentStatusUpdater;

@Entity
@Table
public class Environment implements AuthResource, AccountAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "environment_generator")
    @SequenceGenerator(name = "environment_generator", sequenceName = "environment_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String originalName;

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

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json backup;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json dataServices;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private boolean createFreeIpa;

    private Integer freeIpaInstanceCountByGroup;

    @Convert(converter = FreeIpaLoadBalancerTypeConverter.class)
    private FreeIpaLoadBalancerType freeIpaLoadBalancer;

    private String freeIpaInstanceType;

    private String freeIpaImageCatalog;

    private String freeIpaImageId;

    private String freeIpaImageOs;

    private String freeIpaArchitecture;

    private String freeIpaPlatformVariant;

    @Column(nullable = false)
    private boolean freeIpaEnableMultiAz;

    @Column(nullable = false)
    private String locationDisplayName;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    private boolean archived;

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    @Column(nullable = false)
    private String creator;

    private Long deletionTimestamp = -1L;

    @OneToOne(mappedBy = "environment", cascade = CascadeType.ALL, orphanRemoval = true)
    private BaseNetwork network;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    @Convert(converter = EnvironmentStatusConverter.class)
    private EnvironmentStatus status;

    @Convert(converter = EnvironmentDeletionTypeConverter.class)
    private EnvironmentDeletionType deletionType;

    @JoinColumn(name = "environment_authentication_id")
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private EnvironmentAuthentication authentication;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String statusReason;

    private Long created;

    @Column(length = 4000, name = "securitygroup_id_knox")
    private String securityGroupIdForKnox;

    @Column(length = 4000, name = "securitygroup_id_default")
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

    @Column(name = "environment_service_version")
    private String environmentServiceVersion;

    @Column(name = "environment_domain")
    private String domain;

    @Column(nullable = false, name = "enable_secret_encryption")
    private boolean enableSecretEncryption;

    @Column(name = "creator_client")
    private String creatorClient;

    @Convert(converter = SeLinuxConverter.class)
    @Column(name = "selinux")
    private SeLinux seLinux;

    @Convert(converter = EnvironmentTypeConverter.class)
    @Column(name = "environmentType")
    private EnvironmentType environmentType;

    @Column(name = "remoteenvironmentcrn")
    private String remoteEnvironmentCrn;

    @ManyToOne
    @JoinColumn(name = "encryption_profile_id")
    private EncryptionProfile encryptionProfile;

    @Embedded
    private DefaultComputeCluster defaultComputeCluster;

    public Environment() {
        regions = new Json(new HashSet<Region>());
        tags = new Json(new EnvironmentTags(new HashMap<>(), new HashMap<>()));
        experimentalFeaturesJson = new Json(new ExperimentalFeatures());
    }

    public RegionWrapper getRegionWrapper() {
        Set<String> regions = getRegionSet().stream().map(Region::getName).collect(Collectors.toSet());
        return new RegionWrapper(location, locationDisplayName, latitude, longitude, regions);
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
    public String getResourceName() {
        return name;
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

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
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

    public void setRegions(Json regions) {
        this.regions = regions;
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

    public void setTelemetry(Json telemetry) {
        this.telemetry = telemetry;
    }

    public EnvironmentDataServices getDataServices() {
        if (dataServices != null && dataServices.getValue() != null) {
            return JsonUtil.readValueOpt(dataServices.getValue(), EnvironmentDataServices.class).orElse(null);
        }
        return null;
    }

    public void setDataServices(EnvironmentDataServices dataServices) {
        if (dataServices != null) {
            this.dataServices = new Json(dataServices);
        }
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

    public void setBackup(Json backup) {
        this.backup = backup;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
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

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public String getCreator() {
        return creator;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
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

    public FreeIpaLoadBalancerType getFreeIpaLoadBalancer() {
        return freeIpaLoadBalancer;
    }

    public void setFreeIpaLoadBalancer(FreeIpaLoadBalancerType freeIpaLoadBalancer) {
        this.freeIpaLoadBalancer = freeIpaLoadBalancer;
    }

    public String getFreeIpaInstanceType() {
        return freeIpaInstanceType;
    }

    public void setFreeIpaInstanceType(String freeIpaInstanceType) {
        this.freeIpaInstanceType = freeIpaInstanceType;
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

    public String getFreeIpaImageOs() {
        return freeIpaImageOs;
    }

    public void setFreeIpaImageOs(String freeIpaImageOs) {
        this.freeIpaImageOs = freeIpaImageOs;
    }

    public String getFreeIpaArchitecture() {
        return freeIpaArchitecture;
    }

    public void setFreeIpaArchitecture(String freeIpaArchitecture) {
        this.freeIpaArchitecture = freeIpaArchitecture;
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
        return EnvironmentTags.fromJson(tags);
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

    public String getEnvironmentServiceVersion() {
        return environmentServiceVersion;
    }

    public void setEnvironmentServiceVersion(String environmentServiceVersion) {
        this.environmentServiceVersion = environmentServiceVersion;
    }

    public String getFreeIpaPlatformVariant() {
        return freeIpaPlatformVariant;
    }

    public void setFreeIpaPlatformVariant(String freeIpaPlatformVariant) {
        this.freeIpaPlatformVariant = freeIpaPlatformVariant;
    }

    public boolean isFreeIpaEnableMultiAz() {
        return freeIpaEnableMultiAz;
    }

    public void setFreeIpaEnableMultiAz(boolean freeIpaEnableMultiAz) {
        this.freeIpaEnableMultiAz = freeIpaEnableMultiAz;
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

    public boolean isEnableSecretEncryption() {
        return enableSecretEncryption;
    }

    public void setEnableSecretEncryption(boolean enableSecretEncryption) {
        this.enableSecretEncryption = enableSecretEncryption;
    }

    public String getCreatorClient() {
        return creatorClient;
    }

    public void setCreatorClient(String creatorClient) {
        this.creatorClient = creatorClient;
    }

    public DefaultComputeCluster getDefaultComputeCluster() {
        return defaultComputeCluster;
    }

    public void setDefaultComputeCluster(DefaultComputeCluster defaultComputeCluster) {
        this.defaultComputeCluster = defaultComputeCluster;
    }

    public SeLinux getSeLinux() {
        return seLinux;
    }

    public void setSeLinux(SeLinux seLinux) {
        this.seLinux = seLinux;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(EnvironmentType environmentType) {
        this.environmentType = environmentType;
    }

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public void setRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
    }

    public EncryptionProfile getEncryptionProfile() {
        return encryptionProfile;
    }

    public void setEncryptionProfile(EncryptionProfile encryptionProfile) {
        this.encryptionProfile = encryptionProfile;
    }

    @Override
    public String toString() {
        return "Environment{" +
                "name='" + name + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", freeIpaPlatformVariant='" + freeIpaPlatformVariant + '\'' +
                ", freeIpaEnableMultiAz=" + freeIpaEnableMultiAz + '\'' +
                ", freeIpaLoadBalancer=" + freeIpaLoadBalancer + '\'' +
                ", creator='" + creator + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", status=" + status +
                ", deletionType=" + deletionType +
                ", statusReason='" + statusReason + '\'' +
                ", domain='" + domain + '\'' +
                ", enableSecretEncryption=" + enableSecretEncryption +
                ", seLinux=" + seLinux +
                ", environmentType=" + environmentType +
                ", remoteEnvironmentCrn=" + remoteEnvironmentCrn +
                ", encryptionProfile=" + encryptionProfile +
                '}';
    }

}
