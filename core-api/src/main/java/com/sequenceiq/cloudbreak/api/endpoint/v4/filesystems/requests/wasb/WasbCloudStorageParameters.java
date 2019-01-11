package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidWasbCloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidWasbCloudStorageParameters
public class WasbCloudStorageParameters implements CloudStorageParameters {

    @ApiModelProperty
    @NotNull
    private String accountKey;

    @ApiModelProperty
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

    public boolean isSecure() {
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
        if (!(o instanceof WasbCloudStorageParameters)) {
            return false;
        }
        WasbCloudStorageParameters that = (WasbCloudStorageParameters) o;
        return isSecure() == that.isSecure()
                && Objects.equals(getAccountKey(), that.getAccountKey())
                && Objects.equals(getAccountName(), that.getAccountName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountKey(), getAccountName(), isSecure());
    }

}
