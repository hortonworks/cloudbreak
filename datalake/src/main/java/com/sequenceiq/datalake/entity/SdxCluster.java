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

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.converter.CertExpirationStateConverter;
import com.sequenceiq.cloudbreak.converter.FileSystemTypeConverter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.converter.SdxClusterShapeConverter;
import com.sequenceiq.datalake.converter.SdxDatabaseAvailabilityTypeConverter;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"accountid", "envname"}))
public class SdxCluster implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sdx_cluster_generator")
    @SequenceGenerator(name = "sdx_cluster_generator", sequenceName = "sdxcluster_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    private String accountId;

    private String crn;

    @NotNull
    private String clusterName;

    @NotNull
    private String initiatorUserCrn;

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

    @Convert(converter = CertExpirationStateConverter.class)
    private CertExpirationState certExpirationState = CertExpirationState.VALID;

    @Column(name = "sdx_cluster_service_version")
    private String sdxClusterServiceVersion;

    @Column(name = "cm_ha_enabled")
    private boolean cmHAEnabled;

    public Long getId() {
        return id;
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

    public String getAccountId() {
        return accountId;
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

    public String getInitiatorUserCrn() {
        return initiatorUserCrn;
    }

    public void setInitiatorUserCrn(String initiatorUserCrn) {
        this.initiatorUserCrn = initiatorUserCrn;
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

    public void setStackRequest(String stackRequest) {
        this.stackRequest = new Secret(stackRequest);
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

    public boolean isCmHAEnabled() {
        return cmHAEnabled;
    }

    public void setCmHAEnabled(boolean cmHAEnabled) {
        this.cmHAEnabled = cmHAEnabled;
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
                Objects.equals(initiatorUserCrn, that.initiatorUserCrn) &&
                Objects.equals(envName, that.envName) &&
                Objects.equals(envCrn, that.envCrn) &&
                Objects.equals(stackCrn, that.stackCrn) &&
                clusterShape == that.clusterShape &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(stackId, that.stackId) &&
                Objects.equals(stackRequest, that.stackRequest) &&
                Objects.equals(stackRequestToCloudbreak, that.stackRequestToCloudbreak) &&
                Objects.equals(deleted, that.deleted) &&
                Objects.equals(created, that.created) &&
                Objects.equals(databaseCrn, that.databaseCrn) &&
                Objects.equals(cloudStorageBaseLocation, that.cloudStorageBaseLocation) &&
                cloudStorageFileSystemType == that.cloudStorageFileSystemType &&
                databaseAvailabilityType == that.databaseAvailabilityType &&
                rangerRazEnabled == that.rangerRazEnabled &&
                certExpirationState == that.certExpirationState &&
                Objects.equals(sdxClusterServiceVersion, that.sdxClusterServiceVersion) &&
                cmHAEnabled ==  that.cmHAEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountId, crn, clusterName, initiatorUserCrn, envName, envCrn, stackCrn, clusterShape, tags, stackId, stackRequest,
                stackRequestToCloudbreak, deleted, created, createDatabase, databaseCrn, cloudStorageBaseLocation, cloudStorageFileSystemType,
                databaseAvailabilityType, rangerRazEnabled, certExpirationState, sdxClusterServiceVersion, cmHAEnabled);
    }

    @Override
    public String toString() {
        return "SdxCluster{" +
                "crn='" + crn + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", envName='" + envName + '\'' +
                ", envCrn='" + envCrn + '\'' +
                ", runtime='" + runtime + '\'' +
                ", clusterShape=" + clusterShape +
                ", createDatabase=" + createDatabase +
                ", cloudStorageBaseLocation='" + cloudStorageBaseLocation + '\'' +
                ", rangerRazEnabled=" + rangerRazEnabled +
                ", certExpirationState=" + certExpirationState +
                ", sdxClusterServiceVersion=" + sdxClusterServiceVersion +
                '}';
    }

    //CHECKSTYLE:ON
}
