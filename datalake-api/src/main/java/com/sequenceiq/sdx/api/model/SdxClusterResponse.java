package com.sequenceiq.sdx.api.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxClusterResponse {

    @Schema(description = ModelDescriptions.DATA_LAKE_CRN)
    private String crn;

    @Schema(description = ModelDescriptions.DATA_LAKE_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = ModelDescriptions.CLUSTER_SHAPE, requiredMode = Schema.RequiredMode.REQUIRED)
    private SdxClusterShape clusterShape;

    @Schema(description = ModelDescriptions.DATA_LAKE_STATUS)
    private SdxClusterStatusResponse status;

    @Schema(description = ModelDescriptions.DATA_LAKE_STATUS_REASON)
    private String statusReason;

    @Schema(description = ModelDescriptions.ENVIRONMENT_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentName;

    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN)
    private String environmentCrn;

    @Schema(description = ModelDescriptions.DATABASE_SERVER_CRN)
    private String databaseServerCrn;

    @Schema(description = ModelDescriptions.STACK_CRN)
    private String stackCrn;

    @Schema(description = ModelDescriptions.CREATED)
    private Long created;

    @Schema(description = ModelDescriptions.CLOUD_STORAGE_BASE_LOCATION)
    private String cloudStorageBaseLocation;

    @Schema(description = ModelDescriptions.CLOUD_STORAGE_FILE_SYSTEM_TYPE)
    private FileSystemType cloudStorageFileSystemType;

    @Schema(description = ModelDescriptions.RUNTIME_VERSION)
    private String runtime;

    @Schema(description = ModelDescriptions.FLOW_IDENTIFIER)
    private FlowIdentifier flowIdentifier;

    @Schema(description = ModelDescriptions.RANGER_RAZ_ENABLED, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean rangerRazEnabled;

    @Schema(description = ModelDescriptions.RANGER_RMS_ENABLED, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean rangerRmsEnabled;

    @Schema(description = ModelDescriptions.MULTI_AZ_ENABLED, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean enableMultiAz;

    @Schema(description = ModelDescriptions.TAGS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> tags = new HashMap<>();

    @Schema(description = ClusterModelDescription.CERT_EXPIRATION)
    private CertExpirationState certExpirationState;

    @Schema(description = ClusterModelDescription.CERT_EXPIRATION_DETAILS)
    private String certExpirationDetails;

    @Schema(description = ModelDescriptions.DATA_LAKE_CLUSTER_SERVICE_VERSION)
    private String sdxClusterServiceVersion;

    @Schema(description = ModelDescriptions.DETACHED, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean detached;

    @Schema(description = ModelDescriptions.DATABASE_ENGINE_VERSION)
    private String databaseEngineVersion;

    @Schema(description = ModelDescriptions.DATABASE)
    private SdxDatabaseResponse sdxDatabaseResponse;

    @Schema(description = ModelDescriptions.SECURITY)
    private String seLinuxPolicy;

    @Schema(description = ModelDescriptions.PROVIDER_SYNC_STATES)
    private Set<ProviderSyncState> providerSyncStates = new HashSet<>();

    @Schema(description = ModelDescriptions.DETACHED_CLUSTER_NAME)
    private String detachedClusterName;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public SdxClusterStatusResponse getStatus() {
        return status;
    }

    public void setStatus(SdxClusterStatusResponse status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(SdxClusterShape clusterShape) {
        this.clusterShape = clusterShape;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getDatabaseServerCrn() {
        return databaseServerCrn;
    }

    public void setDatabaseServerCrn(String databaseServerCrn) {
        this.databaseServerCrn = databaseServerCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
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

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public void setRangerRazEnabled(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    public boolean getRangerRazEnabled() {
        return rangerRazEnabled;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public CertExpirationState getCertExpirationState() {
        return certExpirationState;
    }

    public void setCertExpirationState(CertExpirationState certExpirationState) {
        this.certExpirationState = certExpirationState;
    }

    public String getCertExpirationDetails() {
        return certExpirationDetails;
    }

    public void setCertExpirationDetails(String certExpirationDetails) {
        this.certExpirationDetails = certExpirationDetails;
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

    public String getDetachedClusterName() {
        return detachedClusterName;
    }

    public void setDetachedClusterName(String detachedClusterName) {
        this.detachedClusterName = detachedClusterName;
    }

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    public void setDatabaseEngineVersion(String databaseEngineVersion) {
        this.databaseEngineVersion = databaseEngineVersion;
    }

    public String getDatabaseEngineVersion() {
        return databaseEngineVersion;
    }

    public boolean isRangerRazEnabled() {
        return rangerRazEnabled;
    }

    public SdxDatabaseResponse getSdxDatabaseResponse() {
        return sdxDatabaseResponse;
    }

    public void setSdxDatabaseResponse(SdxDatabaseResponse sdxDatabaseResponse) {
        this.sdxDatabaseResponse = sdxDatabaseResponse;
    }

    public boolean isRangerRmsEnabled() {
        return rangerRmsEnabled;
    }

    public void setRangerRmsEnabled(boolean rangerRmsEnabled) {
        this.rangerRmsEnabled = rangerRmsEnabled;
    }

    public String getSeLinuxPolicy() {
        return seLinuxPolicy;
    }

    public void setSeLinuxPolicy(String seLinuxPolicy) {
        this.seLinuxPolicy = seLinuxPolicy;
    }

    public Set<ProviderSyncState> getProviderSyncStates() {
        return providerSyncStates;
    }

    public void setProviderSyncStates(Set<ProviderSyncState> providerSyncStates) {
        this.providerSyncStates = providerSyncStates;
    }

    @Override
    public String toString() {
        return "SdxClusterResponse{" +
                "crn='" + crn + '\'' +
                ", name='" + name + '\'' +
                ", clusterShape=" + clusterShape +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", environmentName='" + environmentName + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", databaseServerCrn='" + databaseServerCrn + '\'' +
                ", stackCrn='" + stackCrn + '\'' +
                ", created=" + created +
                ", cloudStorageBaseLocation='" + cloudStorageBaseLocation + '\'' +
                ", cloudStorageFileSystemType=" + cloudStorageFileSystemType +
                ", runtime='" + runtime + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                ", rangerRazEnabled=" + rangerRazEnabled +
                ", rangerRmsEnabled=" + rangerRmsEnabled +
                ", enableMultiAz=" + enableMultiAz +
                ", tags=" + tags +
                ", seLinuxPolicy=" + seLinuxPolicy +
                ", certExpirationState=" + certExpirationState + '\'' +
                ", certExpirationDetails='" + certExpirationDetails + '\'' +
                ", sdxClusterServiceVersion='" + sdxClusterServiceVersion + '\'' +
                ", detached=" + detached +
                ", detachedClusterName=" + detachedClusterName +
                ", databaseEngineVersion='" + databaseEngineVersion + '\'' +
                ", sdxDatabaseResponse=" + sdxDatabaseResponse +
                ", seLinuxPolicy='" + seLinuxPolicy + '\'' +
                ", providerSyncStates=" + providerSyncStates +
                '}';
    }
}
