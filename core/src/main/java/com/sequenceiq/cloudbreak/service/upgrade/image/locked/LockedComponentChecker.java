package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

@Component
public class LockedComponentChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockedComponentChecker.class);

    @Inject
    private ParcelMatcher parcelMatcher;

    @Inject
    private StackVersionMatcher stackVersionMatcher;

    @Inject
    private CmVersionMatcher cmVersionMatcher;

    public boolean isUpgradePermitted(ClusterUpgradeProperties clusterUpgradeProperties, Map<String, String> activatedParcels) {
        String currentCmBuildNumber = clusterUpgradeProperties.getCurrentPackageVersions().get(ImagePackageVersion.CM_BUILD_NUMBER.getKey());
        boolean parcelsMatch = parcelMatcher.isMatchingNonCdhParcels(clusterUpgradeProperties.getAllTargetProducts(), activatedParcels);
        boolean stackVersionMatches = stackVersionMatcher.isMatchingStackVersion(clusterUpgradeProperties.getCdhParcel(), activatedParcels);
        boolean cmVersionMatches = cmVersionMatcher.isCmVersionMatching(currentCmBuildNumber, clusterUpgradeProperties.getClouderaManagerRepo());
        LOGGER.debug("Checking whether candidate image {} packages match the current activated parcels {} and CM build number {}. "
                        + "Result: parcels match {}, stack version matches {}, CM version matches {}",
                clusterUpgradeProperties.getTargetImageId(), activatedParcels, currentCmBuildNumber, parcelsMatch, stackVersionMatches, cmVersionMatches);
        return parcelsMatch && stackVersionMatches && cmVersionMatches;
    }

    public boolean isUpgradePermitted(Image candidateImage, Map<String, String> activatedParcels, String cmBuildNumber) {
        boolean parcelsMatch = parcelMatcher.isMatchingNonCdhParcels(candidateImage, activatedParcels);
        boolean stackVersionMatches = stackVersionMatcher.isMatchingStackVersion(candidateImage, activatedParcels);
        boolean cmVersionMatches = cmVersionMatcher.isCmVersionMatching(cmBuildNumber, candidateImage);
        LOGGER.debug("Checking whether candidate image {} packages match the current activated parcels {} and CM build number {}. "
                + "Result: parcels match {}, stack version matches {}, CM version matches {}",
                candidateImage.getUuid(), activatedParcels, cmBuildNumber, parcelsMatch, stackVersionMatches, cmVersionMatches);
        return parcelsMatch && stackVersionMatches && cmVersionMatches;
    }
}
