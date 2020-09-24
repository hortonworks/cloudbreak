package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class EntitlementDrivenPackageLocationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntitlementDrivenPackageLocationFilter.class);

    private final EntitlementService entitlementService;

    private final Set<PackageLocationFilter> filters;

    public EntitlementDrivenPackageLocationFilter(EntitlementService entitlementService, Set<PackageLocationFilter> filters) {
        this.entitlementService = entitlementService;
        this.filters = filters;
    }

    public Predicate<Image> filterImage(Image currentImage) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (entitlementService.isInternalRepositoryForUpgradeAllowed(INTERNAL_ACTOR_CRN, accountId)) {
            LOGGER.debug("Skipping image filtering based on repository url");
            return image -> true;
        } else {
            return image -> filters.stream().allMatch(filter -> filter.filterImage(image, currentImage));
        }
    }
}
