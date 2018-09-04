package com.sequenceiq.cloudbreak.api.model.v2.filesystem;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidAbfsCloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidAbfsCloudStorageParameters
public class AbfsCloudStorageParameters implements CloudStorageParameters {

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
        return FileSystemType.ABFS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbfsCloudStorageParameters)) {
            return false;
        }
        AbfsCloudStorageParameters that = (AbfsCloudStorageParameters) o;
        return Objects.equals(getAccountKey(), that.getAccountKey())
                && Objects.equals(getAccountName(), that.getAccountName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountKey(), getAccountName());
    }

}
