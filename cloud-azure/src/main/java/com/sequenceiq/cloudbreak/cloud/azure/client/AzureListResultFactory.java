package com.sequenceiq.cloudbreak.cloud.azure.client;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.Region;
import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsListingByResourceGroup;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsListing;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsListingByRegion;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.RegionUtil;

@Component
public class AzureListResultFactory {

    @Inject
    private AzureExceptionHandler azureExceptionHandler;

    public <T> AzureListResult<T> create(PagedIterable<T> pagedIterable) {
        return new AzureListResult<>(pagedIterable, azureExceptionHandler);
    }

    public <T> AzureListResult<T> list(SupportsListing<T> supportsListing) {
        return create(supportsListing.list());
    }

    public <T> AzureListResult<T> listByRegion(SupportsListingByRegion<T> supportsListingByRegion, Region region) {
        return create(supportsListingByRegion.listByRegion(region));
    }

    public <T> AzureListResult<T> listByRegion(SupportsListingByRegion<T> supportsListingByRegion, String region) {
        return create(supportsListingByRegion.listByRegion(RegionUtil.findByLabelOrName(region)));
    }

    public <T> AzureListResult<T> listByResourceGroup(SupportsListingByResourceGroup<T> supportsListingByResourceGroup, String resourceGroup) {
        return create(supportsListingByResourceGroup.listByResourceGroup(resourceGroup));
    }

}
