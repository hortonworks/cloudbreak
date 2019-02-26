package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.azure;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageParametersV4Response;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidWasbCloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidWasbCloudStorageParameters
public class WasbCloudStorageParametersV4Response implements CloudStorageParametersV4Response {

    @ApiModelProperty
    @NotNull
    private String accountKey;

    @ApiModelProperty
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

    @ApiModelProperty(hidden = true)
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
