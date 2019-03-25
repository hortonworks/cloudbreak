package com.sequenceiq.it.cloudbreak.newway.util.storagelocation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;

public class AzureTestStorageLocation extends TestStorageLocation {

    private final String storageAccountName;

    public AzureTestStorageLocation(String storageAccountName, String clusterName, String baseStorageLocationName) {
        super(baseStorageLocationName, clusterName);
        this.storageAccountName = storageAccountName;
    }

    public Set<StorageLocationV4Request> getAdlsGen2(@Nonnull StorageComponent... components) {
        Set<StorageLocationV4Request> locations = new LinkedHashSet<>();
        Arrays.asList(components).forEach(storageComponent -> locations.addAll(storageComponent.getLocations(this)));
        return locations;
    }

    @Override
    public String getStorageTypePrefix() {
        return "abfs";
    }

    @Override
    public String getPathInfix() {
        return super.getBaseStorageLocationName() + "@" + storageAccountName + ".blob.core.windows.net";
    }

}
