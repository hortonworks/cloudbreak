package com.sequenceiq.common.api.cloudstorage.old;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.api.cloudstorage.old.validation.ValidAdlsCloudStorageParameters;
import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@ValidAdlsCloudStorageParameters
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AdlsCloudStorageV1Parameters implements FileSystemAwareCloudStorage {

    @Schema
    @NotNull
    private String accountName;

    @Schema
    @NotNull
    private String clientId;

    @Schema
    @NotNull
    private String credential;

    @Schema
    private String tenantId;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Schema(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.ADLS;
    }

}