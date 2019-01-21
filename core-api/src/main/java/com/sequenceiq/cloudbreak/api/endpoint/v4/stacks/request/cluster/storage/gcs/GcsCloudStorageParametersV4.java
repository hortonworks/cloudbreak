package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidGcsCloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidGcsCloudStorageParameters
public class GcsCloudStorageParametersV4 implements CloudStorageParametersV4 {

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
        if (!(o instanceof GcsCloudStorageParametersV4)) {
            return false;
        }
        GcsCloudStorageParametersV4 that = (GcsCloudStorageParametersV4) o;
        return Objects.equals(getServiceAccountEmail(), that.getServiceAccountEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServiceAccountEmail());
    }

}
