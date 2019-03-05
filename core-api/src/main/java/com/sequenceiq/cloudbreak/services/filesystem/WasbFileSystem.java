package com.sequenceiq.cloudbreak.services.filesystem;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WasbFileSystem extends BaseFileSystem {

    private String accountKey;

    private String accountName;

    private boolean secure;

    private String storageContainerName;

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

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getStorageContainerName() {
        return storageContainerName;
    }

    public void setStorageContainerName(String storageContainerName) {
        this.storageContainerName = storageContainerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WasbFileSystem)) {
            return false;
        }
        WasbFileSystem that = (WasbFileSystem) o;
        return secure == that.secure
                && Objects.equals(accountKey, that.accountKey)
                && Objects.equals(accountName, that.accountName)
                && Objects.equals(storageContainerName, that.storageContainerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName, secure, storageContainerName);
    }

}
