package com.sequenceiq.common.api.cloudstorage.old;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.common.api.cloudstorage.old.validation.ValidWasbCloudStorageParameters;
import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@ValidWasbCloudStorageParameters
public class WasbCloudStorageV1Parameters implements FileSystemAwareCloudStorage {

    @Schema
    @NotNull
    private String accountKey;

    @Schema
    @NotNull
    private String accountName;

    @Schema
    private boolean secure;

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Schema(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.WASB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WasbCloudStorageV1Parameters)) {
            return false;
        }
        WasbCloudStorageV1Parameters that = (WasbCloudStorageV1Parameters) o;
        return Objects.equals(isSecure(), that.isSecure())
                && Objects.equals(accountKey, that.accountKey)
                && Objects.equals(accountName, that.accountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName, isSecure());
    }

}
