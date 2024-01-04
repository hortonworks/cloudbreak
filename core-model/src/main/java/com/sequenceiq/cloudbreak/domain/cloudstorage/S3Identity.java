package com.sequenceiq.cloudbreak.domain.cloudstorage;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

public class S3Identity implements FileSystemAwareCloudStorage {

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
        if (!(o instanceof S3Identity)) {
            return false;
        }
        S3Identity that = (S3Identity) o;
        return Objects.equals(instanceProfile, that.instanceProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile);
    }

}
