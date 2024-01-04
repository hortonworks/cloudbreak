package com.sequenceiq.environment.environment.dto.telemetry;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

public class WasbCloudStorageParameters implements FileSystemAwareCloudStorage {

    @NotNull
    private String accountKey;

    @NotNull
    private String accountName;

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

    @Override
    public FileSystemType getType() {
        return FileSystemType.WASB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WasbCloudStorageParameters)) {
            return false;
        }
        WasbCloudStorageParameters that = (WasbCloudStorageParameters) o;
        return Objects.equals(isSecure(), that.isSecure())
                && Objects.equals(accountKey, that.accountKey)
                && Objects.equals(accountName, that.accountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName, isSecure());
    }

}
