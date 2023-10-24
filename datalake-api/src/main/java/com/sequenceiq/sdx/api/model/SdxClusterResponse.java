package com.sequenceiq.sdx.api.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxClusterResponse {

    @ApiModelProperty(ModelDescriptions.DATA_LAKE_CRN)
    private String crn;

    @ApiModelProperty(ModelDescriptions.DATA_LAKE_NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.CLUSTER_SHAPE)
    private SdxClusterShape clusterShape;

    @ApiModelProperty(ModelDescriptions.DATA_LAKE_STATUS)
    private SdxClusterStatusResponse status;

    @ApiModelProperty(ModelDescriptions.DATA_LAKE_STATUS_REASON)
    private String statusReason;

    @ApiModelProperty(ModelDescriptions.ENVIRONMENT_NAME)
    private String environmentName;

    @ApiModelProperty(ModelDescriptions.ENVIRONMENT_CRN)
    private String environmentCrn;

    @ApiModelProperty(ModelDescriptions.DATABASE_SERVER_CRN)
    private String databaseServerCrn;

    @ApiModelProperty(ModelDescriptions.STACK_CRN)
    private String stackCrn;

    @ApiModelProperty(ModelDescriptions.CREATED)
    private Long created;

    @ApiModelProperty(ModelDescriptions.CLOUD_STORAGE_BASE_LOCATION)
    private String cloudStorageBaseLocation;

    @ApiModelProperty(ModelDescriptions.CLOUD_STORAGE_FILE_SYSTEM_TYPE)
    private FileSystemType cloudStorageFileSystemType;

    @ApiModelProperty(ModelDescriptions.RUNTIME_VERSION)
    private String runtime;

    @ApiModelProperty(ModelDescriptions.FLOW_IDENTIFIER)
    private FlowIdentifier flowIdentifier;

    @ApiModelProperty(ModelDescriptions.RANGER_RAZ_ENABLED)
    private boolean rangerRazEnabled;

    @ApiModelProperty(ModelDescriptions.RANGER_RMS_ENABLED)
    private boolean rangerRmsEnabled;

    @ApiModelProperty(ModelDescriptions.MULTI_AZ_ENABLED)
    private boolean enableMultiAz;

    @ApiModelProperty(ModelDescriptions.TAGS)
    private Map<String, String> tags;

    @ApiModelProperty(ClusterModelDescription.CERT_EXPIRATION)
    private CertExpirationState certExpirationState;

    @ApiModelProperty(ModelDescriptions.DATA_LAKE_CLUSTER_SERVICE_VERSION)
    private String sdxClusterServiceVersion;

    @ApiModelProperty(ModelDescriptions.DETACHED)
    private boolean detached;

    @ApiModelProperty(ModelDescriptions.DATABASE_ENGINE_VERSION)
    private String databaseEngineVersion;

    @ApiModelProperty(ModelDescriptions.DATABASE)
    private SdxDatabaseResponse sdxDatabaseResponse;

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
                ", certExpirationState=" + certExpirationState +
                ", sdxClusterServiceVersion='" + sdxClusterServiceVersion + '\'' +
                ", detached=" + detached +
                ", databaseEngineVersion='" + databaseEngineVersion + '\'' +
                ", sdxDatabaseResponse=" + sdxDatabaseResponse +
                '}';
    }
}
