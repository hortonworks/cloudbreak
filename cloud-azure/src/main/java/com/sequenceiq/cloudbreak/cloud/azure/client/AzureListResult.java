package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.PagedResponse;
import com.azure.core.management.Region;
import com.azure.resourcemanager.resources.fluentcore.arm.collection.SupportsListingByResourceGroup;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsListing;
import com.azure.resourcemanager.resources.fluentcore.collection.SupportsListingByRegion;
import com.sequenceiq.cloudbreak.cloud.azure.util.RegionUtil;

public class AzureListResult<T> {

    private final PagedIterable<T> pagedIterable;

    public AzureListResult(PagedIterable<T> pagedIterable) {
        this.pagedIterable = Objects.requireNonNull(pagedIterable);
    }

    public Stream<T> getStream() {
        return pagedIterable.stream();
    }

    public List<T> getAll() {
        return pagedIterable.stream().collect(Collectors.toList());
    }

    public List<T> getWhile(Predicate<List<T>> predicate) {
        List<T> result = new ArrayList<>();
        for (PagedResponse<T> page : pagedIterable.iterableByPage()) {
            result.addAll(page.getValue());
            if (predicate.test(result)) {
                return result;
            }
        }
        return result;
    }

    public static <T> AzureListResult<T> list(SupportsListing<T> supportsListing) {
        return new AzureListResult<>(supportsListing.list());
    }

    public static <T> AzureListResult<T> listByRegion(SupportsListingByRegion<T> supportsListingByRegion, Region region) {
        return new AzureListResult<>(supportsListingByRegion.listByRegion(region));
    }

    public static <T> AzureListResult<T> listByRegion(SupportsListingByRegion<T> supportsListingByRegion, String region) {
        return new AzureListResult<>(supportsListingByRegion.listByRegion(RegionUtil.findByLabelOrName(region)));
    }

    public static <T> AzureListResult<T> listByResourceGroup(SupportsListingByResourceGroup<T> supportsListingByResourceGroup, String resourceGroup) {
        return new AzureListResult<>(supportsListingByResourceGroup.listByResourceGroup(resourceGroup));
    }
}
