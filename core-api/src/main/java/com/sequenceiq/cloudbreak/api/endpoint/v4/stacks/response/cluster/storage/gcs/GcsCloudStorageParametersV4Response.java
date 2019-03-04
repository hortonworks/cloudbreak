package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.gcs;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageParametersV4Response;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidGcsCloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidGcsCloudStorageParameters
public class GcsCloudStorageParametersV4Response implements CloudStorageParametersV4Response {

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
        if (!(o instanceof GcsCloudStorageParametersV4Response)) {
            return false;
        }
        GcsCloudStorageParametersV4Response that = (GcsCloudStorageParametersV4Response) o;
        return Objects.equals(serviceAccountEmail, that.serviceAccountEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceAccountEmail);
    }

}
