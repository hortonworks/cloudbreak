package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidAdlsGen2CloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidAdlsGen2CloudStorageParameters
public class AdlsGen2CloudStorageV4Parameters implements CloudStorageV4Parameters {

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
        if (!(o instanceof AdlsGen2CloudStorageV4Parameters)) {
            return false;
        }
        AdlsGen2CloudStorageV4Parameters that = (AdlsGen2CloudStorageV4Parameters) o;
        return Objects.equals(accountKey, that.accountKey)
                && Objects.equals(accountName, that.accountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountKey, accountName);
    }

}
