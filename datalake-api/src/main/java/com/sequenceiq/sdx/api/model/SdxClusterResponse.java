package com.sequenceiq.sdx.api.model;

import com.sequenceiq.authorization.api.EnvironmentCrnAwareApiModel;
import com.sequenceiq.common.model.FileSystemType;

public class SdxClusterResponse implements EnvironmentCrnAwareApiModel {

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

    public SdxClusterResponse() {
    }

    public SdxClusterResponse(String crn, String name, SdxClusterStatusResponse status,
            String statusReason, String environmentName, String environmentCrn, String stackCrn,
            SdxClusterShape clusterShape,
            String cloudStorageBaseLocation, FileSystemType cloudStorageFileSystemType) {
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
}
