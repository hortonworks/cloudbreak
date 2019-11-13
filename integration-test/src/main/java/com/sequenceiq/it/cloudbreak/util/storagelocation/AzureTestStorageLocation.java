package com.sequenceiq.it.cloudbreak.util.storagelocation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;

public class AzureTestStorageLocation extends TestStorageLocation {

    private final String storageAccountName;

    public AzureTestStorageLocation(String storageAccountName, String clusterName, String baseStorageLocationName) {
        super(baseStorageLocationName, clusterName);
        this.storageAccountName = storageAccountName;
    }

    public List<StorageLocationBase> getAdlsGen2(@Nonnull StorageComponent... components) {
        return Arrays.stream(components).flatMap(storageComponent -> storageComponent.getLocations(this).stream()).collect(Collectors.toList());
    }

    @Override
    public String getStorageTypePrefix() {
        return "abfs";
    }

    @Override
    public String getPathInfix() {
        return super.getBaseStorageLocation();
    }

}
