package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.azure;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageParametersV4Response;
import com.sequenceiq.common.model.FileSystemType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class WasbCloudStorageParametersV4Response implements CloudStorageParametersV4Response {

    @Schema
    @NotNull
    private String accountKey;

    @Schema
    @NotNull
    private String accountName;

    private Boolean secure;

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
        if (!(o instanceof WasbCloudStorageParametersV4Response)) {
            return false;
        }
        WasbCloudStorageParametersV4Response that = (WasbCloudStorageParametersV4Response) o;
        return isSecure().booleanValue() == that.isSecure().booleanValue()
                && Objects.equals(accountKey, that.accountKey)
                && Objects.equals(accountName, that.accountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName, isSecure());
    }

}
