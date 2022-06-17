package com.sequenceiq.datalake.entity;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.converter.CertExpirationStateConverter;
import com.sequenceiq.cloudbreak.converter.FileSystemTypeConverter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.converter.SdxClusterShapeConverter;
import com.sequenceiq.datalake.converter.SdxDatabaseAvailabilityTypeConverter;
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

    @Column(nullable = false)
    private boolean createDatabase;

    private String databaseCrn;

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

    @Convert(converter = SdxDatabaseAvailabilityTypeConverter.class)
    private SdxDatabaseAvailabilityType databaseAvailabilityType;

    @Column(name = "ranger_raz_enabled")
    private boolean rangerRazEnabled;

    private boolean enableMultiAz;

    @Convert(converter = CertExpirationStateConverter.class)
    private CertExpirationState certExpirationState = CertExpirationState.VALID;

    @Column(name = "sdx_cluster_service_version")
    private String sdxClusterServiceVersion;

    @Column(nullable = false)
    private boolean detached;

    private String databaseEngineVersion;

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

    public boolean isCreateDatabase() {
        return createDatabase;
    }

    /**
     * @deprecated Kept only for backward compatibility. Use the {@link #setDatabaseAvailabilityType(SdxDatabaseAvailabilityType)} instead.
     */
    @Deprecated
    public void setCreateDatabase(Boolean createDatabase) {
        this.createDatabase = createDatabase;
    }

    public String getDatabaseCrn() {
        return databaseCrn;
    }

    public void setDatabaseCrn(String databaseCrn) {
        this.databaseCrn = databaseCrn;
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

    public SdxDatabaseAvailabilityType getDatabaseAvailabilityType() {
        if (databaseAvailabilityType != null) {
            return databaseAvailabilityType;
        } else {
            if (createDatabase) {
                return SdxDatabaseAvailabilityType.HA;
            } else {
                return SdxDatabaseAvailabilityType.NONE;
            }
        }
    }

    public boolean hasExternalDatabase() {
        return !SdxDatabaseAvailabilityType.NONE.equals(getDatabaseAvailabilityType());
    }

    public void setDatabaseAvailabilityType(SdxDatabaseAvailabilityType databaseAvailabilityType) {
        this.databaseAvailabilityType = databaseAvailabilityType;
        if (SdxDatabaseAvailabilityType.NONE.equals(databaseAvailabilityType)) {
            createDatabase = false;
        } else {
            createDatabase = true;
        }
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

    public String getDatabaseEngineVersion() {
        return databaseEngineVersion;
    }

    public void setDatabaseEngineVersion(String databaseEngineVersion) {
        this.databaseEngineVersion = databaseEngineVersion;
    }

    //CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SdxCluster that = (SdxCluster) o;
        return createDatabase == that.createDatabase &&
                Objects.equals(id, that.id) &&
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
                Objects.equals(databaseCrn, that.databaseCrn) &&
                Objects.equals(cloudStorageBaseLocation, that.cloudStorageBaseLocation) &&
                Objects.equals(enableMultiAz, that.enableMultiAz) &&
                cloudStorageFileSystemType == that.cloudStorageFileSystemType &&
                databaseAvailabilityType == that.databaseAvailabilityType &&
                rangerRazEnabled == that.rangerRazEnabled &&
                certExpirationState == that.certExpirationState &&
                Objects.equals(sdxClusterServiceVersion, that.sdxClusterServiceVersion) &&
                Objects.equals(databaseEngineVersion, that.databaseEngineVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountId, crn, clusterName, envName, envCrn, stackCrn, clusterShape, tags, stackId, stackRequest,
                stackRequestToCloudbreak, deleted, created, createDatabase, databaseCrn, cloudStorageBaseLocation, cloudStorageFileSystemType,
                databaseAvailabilityType, rangerRazEnabled, certExpirationState, sdxClusterServiceVersion, enableMultiAz, databaseEngineVersion);
    }

    @Override
    public String toString() {
        return "SdxCluster{" +
                "crn='" + crn + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", envName='" + envName + '\'' +
                ", envCrn='" + envCrn + '\'' +
                ", runtime='" + runtime + '\'' +
                ", Detached='" + detached + '\'' +
                ", clusterShape=" + clusterShape +
                ", createDatabase=" + createDatabase +
                ", cloudStorageBaseLocation='" + cloudStorageBaseLocation + '\'' +
                ", rangerRazEnabled=" + rangerRazEnabled +
                ", certExpirationState=" + certExpirationState +
                ", sdxClusterServiceVersion=" + sdxClusterServiceVersion +
                ", enableMultiAz=" + enableMultiAz +
                ", databaseEngineVersion=" + databaseEngineVersion +
                '}';
    }

    //CHECKSTYLE:ON
}
