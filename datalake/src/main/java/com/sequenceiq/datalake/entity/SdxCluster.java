package com.sequenceiq.datalake.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.common.notification.NotificationStateConverter;
import com.sequenceiq.cloudbreak.converter.ArchitectureConverter;
import com.sequenceiq.cloudbreak.converter.CertExpirationStateConverter;
import com.sequenceiq.cloudbreak.converter.FileSystemTypeConverter;
import com.sequenceiq.cloudbreak.converter.ProviderSyncSetToStringConverter;
import com.sequenceiq.cloudbreak.converter.SeLinuxConverter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.converter.SdxClusterShapeConverter;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"accountid", "envname"}))
@EntityType(entityClass = SdxCluster.class)
public class SdxCluster implements AccountAwareResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCluster.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sdx_cluster_generator")
    @SequenceGenerator(name = "sdx_cluster_generator", sequenceName = "sdxcluster_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    private String accountId;

    private String crn;

    // make this entity comply with AccountAwareResourceRepository lookup methods
    @Column(name = "crn", insertable = false, updatable = false)
    private String resourceCrn;

    @Column(name = "originalcrn")
    private String originalCrn;

    @NotNull
    private String clusterName;

    // make this entity comply with AccountAwareResourceRepository lookup methods
    @Column(name = "clusterName", insertable = false, updatable = false)
    private String name;

    @NotNull
    private String envName;

    @NotNull
    private String envCrn;

    private String stackCrn;

    private String runtime;

    @NotNull
    @Convert(converter = SdxClusterShapeConverter.class)
    private SdxClusterShape clusterShape;

    @NotNull
    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    private Long stackId;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret stackRequest = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret stackRequestToCloudbreak = Secret.EMPTY;

    private Long deleted;

    private Long created;

    @Column(columnDefinition = "TEXT")
    private String cloudStorageBaseLocation;

    @Convert(converter = FileSystemTypeConverter.class)
    private FileSystemType cloudStorageFileSystemType;

    /**
     * @deprecated Kept only to avoid downtime
     */
    @Deprecated
    private String repairFlowChainId;

    private String lastCbFlowChainId;

    private String lastCbFlowId;

    @Column(name = "ranger_raz_enabled")
    private boolean rangerRazEnabled;

    @Column(name = "ranger_rms_enabled")
    private boolean rangerRmsEnabled;

    private boolean enableMultiAz;

    @Convert(converter = CertExpirationStateConverter.class)
    private CertExpirationState certExpirationState = CertExpirationState.VALID;

    @Column(name = "sdx_cluster_service_version")
    private String sdxClusterServiceVersion;

    @Column(nullable = false)
    private boolean detached;

    @NotNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sdxdatabase_id")
    private SdxDatabase sdxDatabase;

    @Column(name = "creator_client")
    private String creatorClient;

    @Convert(converter = SeLinuxConverter.class)
    @Column(name = "selinux")
    private SeLinux seLinux;

    @Convert(converter = ArchitectureConverter.class)
    @Column(name = "architecture")
    private Architecture architecture;

    private String certExpirationDetails;

    @Convert(converter = ProviderSyncSetToStringConverter.class)
    private Set<ProviderSyncState> providerSyncStates = new HashSet<>();

    @Convert(converter = NotificationStateConverter.class)
    @Column(name = "notificationstate")
    private NotificationState notificationState;

    public Long getId() {
        return id;
    }

    @Override
    public String getResourceCrn() {
        return crn;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getOriginalCrn() {
        return originalCrn;
    }

    public void setOriginalCrn(String originalCrn) {
        this.originalCrn = originalCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    @Override
    public String getName() {
        return clusterName;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getEnvCrn() {
        return envCrn;
    }

    public void setEnvCrn(String envCrn) {
        this.envCrn = envCrn;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(SdxClusterShape clusterShape) {
        this.clusterShape = clusterShape;
    }

    public Json getTags() {
        return tags;
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(Long deleted) {
        this.deleted = deleted;
    }

    public String getStackRequest() {
        return stackRequest.getRaw();
    }

    public void setStackRequest(StackV4Request stackRequest) {
        try {
            this.stackRequest = new Secret(JsonUtil.writeValueAsString(stackRequest));
        } catch (JsonProcessingException e) {
            LOGGER.error("Can not parse internal stack request", e);
            throw new BadRequestException("Can not parse internal stack request", e);
        }
    }

    public String getCreatorClient() {
        return creatorClient;
    }

    public void setCreatorClient(String creatorClient) {
        this.creatorClient = creatorClient;
    }

    /**
     * Need this for Jackson deserialization
     */
    private void setStackRequest(String rawRequest) {
        this.stackRequest = new Secret(rawRequest);
    }

    public String getStackRequestToCloudbreak() {
        return stackRequestToCloudbreak.getRaw();
    }

    public void setStackRequestToCloudbreak(String stackRequestToCloudbreak) {
        this.stackRequestToCloudbreak = new Secret(stackRequestToCloudbreak);
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getCloudStorageBaseLocation() {
        return cloudStorageBaseLocation;
    }

    public void setCloudStorageBaseLocation(String cloudStorageBaseLocation) {
        this.cloudStorageBaseLocation = cloudStorageBaseLocation;
    }

    public FileSystemType getCloudStorageFileSystemType() {
        return cloudStorageFileSystemType;
    }

    public void setCloudStorageFileSystemType(FileSystemType cloudStorageFileSystemType) {
        this.cloudStorageFileSystemType = cloudStorageFileSystemType;
    }

    public String getLastCbFlowChainId() {
        return lastCbFlowChainId;
    }

    public void setLastCbFlowChainId(String lastCbFlowChainId) {
        this.lastCbFlowChainId = lastCbFlowChainId;
    }

    public String getLastCbFlowId() {
        return lastCbFlowId;
    }

    public void setLastCbFlowId(String lastCbFlowId) {
        this.lastCbFlowId = lastCbFlowId;
    }

    public CertExpirationState getCertExpirationState() {
        return certExpirationState;
    }

    public void setCertExpirationState(CertExpirationState certExpirationState) {
        this.certExpirationState = certExpirationState;
    }

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    @JsonIgnore
    public boolean isCreateDatabase() {
        return sdxDatabase.isCreateDatabase();
    }

    public boolean isRangerRazEnabled() {
        return rangerRazEnabled;
    }

    public void setRangerRazEnabled(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    public String getSdxClusterServiceVersion() {
        return sdxClusterServiceVersion;
    }

    public void setSdxClusterServiceVersion(String sdxClusterServiceVersion) {
        this.sdxClusterServiceVersion = sdxClusterServiceVersion;
    }

    public boolean isDetached() {
        return detached;
    }

    public void setDetached(boolean detached) {
        this.detached = detached;
    }

    @JsonIgnore
    public String getDatabaseCrn() {
        return sdxDatabase.getDatabaseCrn();
    }

    @JsonIgnore
    public SdxDatabaseAvailabilityType getDatabaseAvailabilityType() {
        return sdxDatabase.getDatabaseAvailabilityType();
    }

    @JsonIgnore
    public boolean hasExternalDatabase() {
        return sdxDatabase.hasExternalDatabase();
    }

    @JsonIgnore
    public String getDatabaseEngineVersion() {
        return sdxDatabase.getDatabaseEngineVersion();
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepairFlowChainId() {
        return repairFlowChainId;
    }

    public void setRepairFlowChainId(String repairFlowChainId) {
        this.repairFlowChainId = repairFlowChainId;
    }

    public SdxDatabase getSdxDatabase() {
        return sdxDatabase;
    }

    public void setSdxDatabase(SdxDatabase sdxDatabase) {
        this.sdxDatabase = sdxDatabase;
    }

    public boolean isRangerRmsEnabled() {
        return rangerRmsEnabled;
    }

    public void setRangerRmsEnabled(boolean rangerRmsEnabled) {
        this.rangerRmsEnabled = rangerRmsEnabled;
    }

    public SeLinux getSeLinux() {
        return seLinux;
    }

    public void setSeLinux(SeLinux seLinux) {
        this.seLinux = seLinux;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public void setArchitecture(Architecture architecture) {
        this.architecture = architecture;
    }

    public String getCertExpirationDetails() {
        return certExpirationDetails;
    }

    public void setCertExpirationDetails(String certExpirationDetails) {
        this.certExpirationDetails = certExpirationDetails;
    }

    public Set<ProviderSyncState> getProviderSyncStates() {
        return providerSyncStates;
    }

    public void setProviderSyncStates(Set<ProviderSyncState> providerSyncStates) {
        this.providerSyncStates = providerSyncStates;
    }

    public NotificationState getNotificationState() {
        return notificationState;
    }

    public void setNotificationState(NotificationState notificationState) {
        this.notificationState = notificationState;
    }

    //CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SdxCluster that = (SdxCluster) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(crn, that.crn) &&
                Objects.equals(clusterName, that.clusterName) &&
                Objects.equals(envName, that.envName) &&
                Objects.equals(envCrn, that.envCrn) &&
                Objects.equals(stackCrn, that.stackCrn) &&
                clusterShape == that.clusterShape &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(stackId, that.stackId) &&
                Objects.equals(stackRequest, that.stackRequest) &&
                Objects.equals(stackRequestToCloudbreak, that.stackRequestToCloudbreak) &&
                Objects.equals(deleted, that.deleted) &&
                Objects.equals(detached, that.detached) &&
                Objects.equals(created, that.created) &&
                Objects.equals(cloudStorageBaseLocation, that.cloudStorageBaseLocation) &&
                Objects.equals(enableMultiAz, that.enableMultiAz) &&
                cloudStorageFileSystemType == that.cloudStorageFileSystemType &&
                rangerRazEnabled == that.rangerRazEnabled &&
                rangerRmsEnabled == that.rangerRmsEnabled &&
                certExpirationState == that.certExpirationState &&
                Objects.equals(sdxClusterServiceVersion, that.sdxClusterServiceVersion) &&
                seLinux == that.seLinux &&
                notificationState == that.notificationState &&
                Objects.equals(sdxDatabase, that.sdxDatabase) &&
                Objects.equals(providerSyncStates, that.providerSyncStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountId, crn, clusterName, envName, envCrn, stackCrn, clusterShape, tags, stackId, stackRequest,
                stackRequestToCloudbreak, deleted, created, cloudStorageBaseLocation, cloudStorageFileSystemType, seLinux,
                rangerRazEnabled, rangerRmsEnabled, certExpirationState, sdxClusterServiceVersion, enableMultiAz,
                providerSyncStates, notificationState);
    }

    @Override
    public String toString() {
        return "SdxCluster{" +
                "id=" + id +
                ", accountId='" + accountId + '\'' +
                ", crn='" + crn + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", originalCrn='" + originalCrn + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", name='" + name + '\'' +
                ", envName='" + envName + '\'' +
                ", envCrn='" + envCrn + '\'' +
                ", stackCrn='" + stackCrn + '\'' +
                ", runtime='" + runtime + '\'' +
                ", clusterShape=" + clusterShape +
                ", tags=" + tags +
                ", stackId=" + stackId +
                ", stackRequest=" + stackRequest +
                ", stackRequestToCloudbreak=" + stackRequestToCloudbreak +
                ", deleted=" + deleted +
                ", created=" + created +
                ", cloudStorageBaseLocation='" + cloudStorageBaseLocation + '\'' +
                ", cloudStorageFileSystemType=" + cloudStorageFileSystemType +
                ", repairFlowChainId='" + repairFlowChainId + '\'' +
                ", lastCbFlowChainId='" + lastCbFlowChainId + '\'' +
                ", lastCbFlowId='" + lastCbFlowId + '\'' +
                ", rangerRazEnabled=" + rangerRazEnabled +
                ", rangerRmsEnabled+" + rangerRmsEnabled +
                ", enableMultiAz=" + enableMultiAz +
                ", certExpirationState=" + certExpirationState +
                ", notificationState=" + notificationState +
                ", seLinux=" + seLinux +
                ", sdxClusterServiceVersion='" + sdxClusterServiceVersion + '\'' +
                ", detached=" + detached +
                ", sdxDatabase=" + sdxDatabase +
                ", certExpirationDetails='" + certExpirationDetails +
                ", providerSyncStates=" + providerSyncStates +
                '}';
    }

    //CHECKSTYLE:ON
}
