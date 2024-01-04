package com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.UtilDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StructuredParametersQueryV4Request {

    @NotNull
    @Schema(description = StackModelDescription.STACK_NAME, required = true)
    private String clusterName;

    @Schema(description = UtilDescription.ACCOUNT_NAME)
    private String accountName;

    @NotNull
    @Schema(description = UtilDescription.STORAGE_NAME, required = true)
    private String storageName;

    @NotNull
    @Schema(description = UtilDescription.FILESYTEM_TYPE, required = true)
    private String fileSystemType;

    @Schema(description = UtilDescription.ATTACHED_CLUSTER, required = true)
    private boolean attachedCluster;

    @Schema(description = UtilDescription.SECURE)
    private boolean secure;

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

    public boolean isAttachedCluster() {
        return attachedCluster;
    }

    public void setAttachedCluster(boolean attachedCluster) {
        this.attachedCluster = attachedCluster;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }
}
