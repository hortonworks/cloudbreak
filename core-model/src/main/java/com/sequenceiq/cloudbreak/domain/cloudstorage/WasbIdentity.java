package com.sequenceiq.cloudbreak.domain.cloudstorage;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

public class WasbIdentity implements FileSystemAwareCloudStorage {

    @NotNull
    private String accountKey;

    @NotNull
    private String accountName;

    private String storageContainerName;

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

    public String getStorageContainerName() {
        return storageContainerName;
    }

    public void setStorageContainerName(String storageContainerName) {
        this.storageContainerName = storageContainerName;
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
        if (!(o instanceof WasbIdentity)) {
            return false;
        }
        WasbIdentity that = (WasbIdentity) o;
        return Objects.equals(isSecure(), that.isSecure())
                && Objects.equals(accountKey, that.accountKey)
                && Objects.equals(accountName, that.accountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName, isSecure());
    }

}
