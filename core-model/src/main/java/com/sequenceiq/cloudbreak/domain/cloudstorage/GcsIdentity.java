package com.sequenceiq.cloudbreak.domain.cloudstorage;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

public class GcsIdentity implements FileSystemAwareCloudStorage {

    @NotNull
    private String serviceAccountEmail;

    @Override
    public FileSystemType getType() {
        return FileSystemType.GCS;
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GcsIdentity)) {
            return false;
        }
        GcsIdentity that = (GcsIdentity) o;
        return Objects.equals(serviceAccountEmail, that.serviceAccountEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceAccountEmail);
    }

}
