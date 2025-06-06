package com.sequenceiq.common.api.cloudstorage.old;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.common.api.cloudstorage.old.validation.ValidS3CloudStorageParameters;
import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@ValidS3CloudStorageParameters
public class EfsCloudStorageV1Parameters implements S3CloudStorageParameterBase, FileSystemAwareCloudStorage {

    @Schema
    @NotNull
    private String instanceProfile;

    @Schema(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.EFS;
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
        if (!(o instanceof EfsCloudStorageV1Parameters)) {
            return false;
        }
        EfsCloudStorageV1Parameters that = (EfsCloudStorageV1Parameters) o;
        return Objects.equals(instanceProfile, that.instanceProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile);
    }

    @Override
    public String toString() {
        return "EfsCloudStorageV1Parameters{" +
                "instanceProfile='" + instanceProfile + '\'' +
                '}';
    }
}
