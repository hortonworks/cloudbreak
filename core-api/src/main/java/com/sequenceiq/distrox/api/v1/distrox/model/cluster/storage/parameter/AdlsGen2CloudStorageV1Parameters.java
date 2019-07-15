package com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.parameter;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.api.cloudstorage.validation.ValidAdlsGen2CloudStorageParameters;
import com.sequenceiq.common.model.FileSystemType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidAdlsGen2CloudStorageParameters
public class AdlsGen2CloudStorageV1Parameters implements CloudStorageV1Parameters {

    @ApiModelProperty
    @NotNull
    private String accountKey;

    @ApiModelProperty
    @NotNull
    private String accountName;

    @ApiModelProperty
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
        return FileSystemType.ADLS_GEN_2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdlsGen2CloudStorageV1Parameters)) {
            return false;
        }
        AdlsGen2CloudStorageV1Parameters that = (AdlsGen2CloudStorageV1Parameters) o;
        return secure == that.secure
                && Objects.equals(accountKey, that.accountKey)
                && Objects.equals(accountName, that.accountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName, secure);
    }
}
