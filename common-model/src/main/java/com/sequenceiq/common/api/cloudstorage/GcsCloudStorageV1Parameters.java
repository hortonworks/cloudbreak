package com.sequenceiq.common.api.cloudstorage;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.api.cloudstorage.validation.ValidGcsCloudStorageParameters;
import com.sequenceiq.common.api.filesystem.FileSystemType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidGcsCloudStorageParameters
public class GcsCloudStorageV1Parameters implements CloudStorageV1Parameters {

    @ApiModelProperty
    @NotNull
    private String serviceAccountEmail;

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    @ApiModelProperty(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.GCS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GcsCloudStorageV1Parameters)) {
            return false;
        }
        GcsCloudStorageV1Parameters that = (GcsCloudStorageV1Parameters) o;
        return Objects.equals(serviceAccountEmail, that.serviceAccountEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceAccountEmail);
    }

}
