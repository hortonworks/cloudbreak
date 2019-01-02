package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

public class FileSystemParametersV4Filter  {

    @NotNull
    @QueryParam("blueprintName")
    private String blueprintName;

    @NotNull
    @QueryParam("clusterName")
    private String clusterName;

    @QueryParam("accountName")
    private String accountName;

    @NotNull
    @QueryParam("storageName")
    private String storageName;

    @NotNull
    @QueryParam("fileSystemType")
    private String fileSystemType;

    @QueryParam("attachedCluster")
    private Boolean attachedCluster;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public String getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(String fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public Boolean isAttachedCluster() {
        return attachedCluster;
    }

    public void setAttachedCluster(Boolean attachedCluster) {
        this.attachedCluster = attachedCluster;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }
}
