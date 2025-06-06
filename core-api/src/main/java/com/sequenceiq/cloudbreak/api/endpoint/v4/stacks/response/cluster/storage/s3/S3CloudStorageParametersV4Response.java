package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.s3;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageParametersV4Response;
import com.sequenceiq.common.model.FileSystemType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class S3CloudStorageParametersV4Response implements CloudStorageParametersV4Response {

    @Schema
    @NotNull
    private String instanceProfile;

    @Schema(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.S3;
    }

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof S3CloudStorageParametersV4Response)) {
            return false;
        }
        S3CloudStorageParametersV4Response that = (S3CloudStorageParametersV4Response) o;
        return Objects.equals(instanceProfile, that.instanceProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile);
    }

}
