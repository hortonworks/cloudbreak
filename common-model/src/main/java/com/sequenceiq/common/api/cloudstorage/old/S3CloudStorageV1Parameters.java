package com.sequenceiq.common.api.cloudstorage.old;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.common.api.cloudstorage.old.validation.ValidS3CloudStorageParameters;
import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@ValidS3CloudStorageParameters
public class S3CloudStorageV1Parameters implements S3CloudStorageParameterBase, FileSystemAwareCloudStorage {

    @Schema
    @NotNull
    private String instanceProfile;

    @Schema(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.S3;
    }

    @Override
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
        if (!(o instanceof S3CloudStorageV1Parameters)) {
            return false;
        }
        S3CloudStorageV1Parameters that = (S3CloudStorageV1Parameters) o;
        return Objects.equals(instanceProfile, that.instanceProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile);
    }

    @Override
    public String toString() {
        return "S3CloudStorageV1Parameters{" +
                "instanceProfile='" + instanceProfile + '\'' +
                '}';
    }
}
