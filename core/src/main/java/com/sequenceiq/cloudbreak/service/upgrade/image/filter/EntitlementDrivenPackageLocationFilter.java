package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.image.PackageLocationFilter;

@Component
public class EntitlementDrivenPackageLocationFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntitlementDrivenPackageLocationFilter.class);

    private static final int ORDER_NUMBER = 8;

    private final EntitlementService entitlementService;

    private final Set<PackageLocationFilter> filters;

    public EntitlementDrivenPackageLocationFilter(EntitlementService entitlementService, Set<PackageLocationFilter> filters) {
        this.entitlementService = entitlementService;
        this.filters = filters;
    }

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, imageFilterParams);
        logNotEligibleImages(imageFilterResult, filteredImages, LOGGER);
        LOGGER.debug("After the filtering {} image left with proper package location", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return "There are no eligible images to upgrade because the location of the packages is not appropriate.";
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        return imageFilterResult.getImages()
                .stream()
                .filter(filterByLocation(imageFilterParams))
                .collect(Collectors.toList());
    }

    private Predicate<Image> filterByLocation(ImageFilterParams imageFilterParams) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (entitlementService.isInternalRepositoryForUpgradeAllowed(accountId)) {
            LOGGER.debug("Skipping image filtering based on repository url");
            return image -> true;
        } else {
            return image -> filters.stream().allMatch(filter -> filter.filterImage(image, imageFilterParams.getCurrentImage(), imageFilterParams));
        }
    }
}
