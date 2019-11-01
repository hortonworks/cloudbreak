package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;

import java.util.List;

public abstract class CloudFileSystemView {

    private final CloudIdentityType cloudIdentityType;

    private AccountMappingBase accountMapping;

    private List<StorageLocationBase> locations;

    protected CloudFileSystemView(CloudIdentityType cloudIdentityType) {
        this.cloudIdentityType = cloudIdentityType;
    }

    public CloudIdentityType getCloudIdentityType() {
        return cloudIdentityType;
    }

    public void setAccountMapping(AccountMappingBase accountMapping) {
        this.accountMapping = accountMapping;
    }

    public AccountMappingBase getAccountMapping() {
        return accountMapping;
    }

    public void setLocations(List<StorageLocationBase> locations) {
        this.locations = locations;
    }

    public List<StorageLocationBase> getLocations() {
        return locations;
    }
}
