package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidAdlsGen2CloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidAdlsGen2CloudStorageParameters
public class AdlsGen2CloudStorageParameters implements CloudStorageParameters {

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
        if (!(o instanceof AdlsGen2CloudStorageParameters)) {
            return false;
        }
        AdlsGen2CloudStorageParameters that = (AdlsGen2CloudStorageParameters) o;
        return Objects.equals(getAccountKey(), that.getAccountKey())
                && Objects.equals(getAccountName(), that.getAccountName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountKey(), getAccountName());
    }

}
