package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.azure;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageParametersV4Response;
import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidAdlsGen2CloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidAdlsGen2CloudStorageParameters
public class AdlsGen2CloudStorageParametersV4Response implements CloudStorageParametersV4Response {

    @ApiModelProperty
    @NotNull
    private String accountKey;

    @ApiModelProperty
    @NotNull
    private String accountName;

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
        if (!(o instanceof AdlsGen2CloudStorageParametersV4Response)) {
            return false;
        }
        AdlsGen2CloudStorageParametersV4Response that = (AdlsGen2CloudStorageParametersV4Response) o;
        return Objects.equals(accountKey, that.accountKey)
                && Objects.equals(accountName, that.accountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName);
    }

}
