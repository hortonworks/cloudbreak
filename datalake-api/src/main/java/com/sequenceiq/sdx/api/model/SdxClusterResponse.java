package com.sequenceiq.sdx.api.model;

import java.util.Map;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.ApiModelProperty;

public class SdxClusterResponse {

    private String crn;

    private String name;

    private SdxClusterShape clusterShape;

    private SdxClusterStatusResponse status;

    private String statusReason;

    private String environmentName;

    private String environmentCrn;

    private String databaseServerCrn;

    private String stackCrn;

    private Long created;

    private String cloudStorageBaseLocation;

    private FileSystemType cloudStorageFileSystemType;

    private String runtime;

    private FlowIdentifier flowIdentifier;

    private boolean rangerRazEnabled;

    private Map<String, String> tags;

    @ApiModelProperty(ClusterModelDescription.CERT_EXPIRATION)
    private CertExpirationState certExpirationState;

    private String sdxClusterServiceVersion;

    private boolean detached;

    public SdxClusterResponse() {
    }

    public SdxClusterResponse(String crn, String name, SdxClusterStatusResponse status,
            String statusReason, String environmentName, String environmentCrn, String stackCrn,
            SdxClusterShape clusterShape, String cloudStorageBaseLocation,
            FileSystemType cloudStorageFileSystemType, String runtime,
            boolean rangerRazEnabled, Map<String, String> tags, CertExpirationState certExpirationState,
            String sdxClusterServiceVersion, boolean detached) {
        this.crn = crn;
        this.name = name;
        this.status = status;
        this.statusReason = statusReason;
        this.environmentName = environmentName;
        this.environmentCrn = environmentCrn;
        this.stackCrn = stackCrn;
        this.clusterShape = clusterShape;
        this.cloudStorageBaseLocation = cloudStorageBaseLocation;
        this.cloudStorageFileSystemType = cloudStorageFileSystemType;
        this.runtime = runtime;
        this.rangerRazEnabled = rangerRazEnabled;
        this.tags = tags;
        this.certExpirationState = certExpirationState;
        this.sdxClusterServiceVersion = sdxClusterServiceVersion;
        this.detached = detached;
    }

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
                ", tags=" + tags +
                ", certExpirationState=" + certExpirationState +
                ", sdxClusterServiceVersion=" + sdxClusterServiceVersion +
                ", Detached=" + detached +
                '}';
    }
}
