package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class LockedComponentChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockedComponentChecker.class);

    @Inject
    private ParcelMatcher parcelMatcher;

    @Inject
    private StackVersionMatcher stackVersionMatcher;

    @Inject
    private CmVersionMatcher cmVersionMatcher;

    public boolean isUpgradePermitted(Image candidateImage, Map<String, String> activatedParcels, String cmBuildNumber) {
        boolean parcelsMatch = parcelMatcher.isMatchingNonCdhParcels(candidateImage, activatedParcels);
        boolean stackVersionMatches = stackVersionMatcher.isMatchingStackVersion(candidateImage, activatedParcels);
        boolean cmVersionMatches = cmVersionMatcher.isCmVersionMatching(cmBuildNumber, candidateImage);
        LOGGER.debug("Validating the candidate image {} packages are matching with the current image packages {} CM build number: {}. "
                + "The result: parcels matches {}, stack version matches {}, CM version matches {}",
                candidateImage.getUuid(), activatedParcels, cmBuildNumber, parcelsMatch, stackVersionMatches, cmVersionMatches);
        return parcelsMatch && stackVersionMatches && cmVersionMatches;
    }
}
