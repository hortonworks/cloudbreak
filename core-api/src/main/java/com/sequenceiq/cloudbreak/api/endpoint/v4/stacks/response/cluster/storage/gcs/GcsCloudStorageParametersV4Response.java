package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.gcs;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageParametersV4Response;
import com.sequenceiq.common.model.FileSystemType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class GcsCloudStorageParametersV4Response implements CloudStorageParametersV4Response {

    @Schema
    @NotNull
    private String serviceAccountEmail;

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    @Schema(hidden = true)
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
