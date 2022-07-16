package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;

public abstract class CloudFileSystemView {

    private final CloudIdentityType cloudIdentityType;

    private AccountMappingBase accountMapping;

    private List<StorageLocationBase> locations;

    @JsonCreator
    protected CloudFileSystemView(@JsonProperty("cloudIdentityType") CloudIdentityType cloudIdentityType) {
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
