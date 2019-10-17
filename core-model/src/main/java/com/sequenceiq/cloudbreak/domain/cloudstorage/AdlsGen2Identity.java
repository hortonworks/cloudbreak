package com.sequenceiq.cloudbreak.domain.cloudstorage;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

public class AdlsGen2Identity implements FileSystemAwareCloudStorage {

    @NotNull
    private String managedIdentity;

    @Override
    public FileSystemType getType() {
        return FileSystemType.ADLS_GEN_2;
    }

    public String getManagedIdentity() {
        return managedIdentity;
    }

    public void setManagedIdentity(String managedIdentity) {
        this.managedIdentity = managedIdentity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdlsGen2Identity)) {
            return false;
        }
        AdlsGen2Identity that = (AdlsGen2Identity) o;
        return Objects.equals(managedIdentity, that.managedIdentity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(managedIdentity);
    }

}
