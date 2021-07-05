package com.sequenceiq.environment.environment.dto.telemetry;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

public class S3CloudStorageParameters implements FileSystemAwareCloudStorage {

    @NotNull
    private String instanceProfile;

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
        if (!(o instanceof S3CloudStorageParameters)) {
            return false;
        }
        S3CloudStorageParameters that = (S3CloudStorageParameters) o;
        return Objects.equals(instanceProfile, that.instanceProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile);
    }

    @Override
    public String toString() {
        return "S3CloudStorageParameters{" +
                "instanceProfile='" + instanceProfile + '\'' +
                '}';
    }
}
