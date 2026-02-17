package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;

@Component
public class CmAndStackVersionUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmAndStackVersionUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 7;

    @Inject
    private LockedComponentChecker lockedComponentChecker;

    @Inject
    private UpgradePermissionProvider upgradePermissionProvider;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            Image candidateImage = imageFilterResult.getImages().getFirst();
            CmAndStackVersionMatchResult cmAndStackVersionMatchResult = isUpgradePermitted(imageFilterParams, candidateImage);
            return getImageFilterResultForTargetImage(imageFilterResult, imageFilterParams, cmAndStackVersionMatchResult, candidateImage);
        } else {
            List<Image> filteredImages =
                    filterImages(imageFilterResult, image -> CmAndStackVersionMatchResult.PERMITTED.equals(isUpgradePermitted(imageFilterParams, image)));
            logNotEligibleImages(imageFilterResult, filteredImages, LOGGER);
            LOGGER.debug("After the filtering {} image left with proper CM and CDH version.", filteredImages.size());
            return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
        }
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return imageFilterParams.isLockComponents()
                ? "There is at least one activated parcel for which we cannot find image with matching version. Activated parcel(s): "
                + imageFilterParams.getStackRelatedParcels()
                : "This service is using the latest available version.";
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private ImageFilterResult getImageFilterResultForTargetImage(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams,
            CmAndStackVersionMatchResult cmAndStackVersionMatchResult, Image candidateImage) {
        return switch (cmAndStackVersionMatchResult) {
            case PERMITTED -> imageFilterResult;
            case LOCKED_COMPONENTS_MISMATCH -> new ImageFilterResult(List.of(), getCantUpgradeToImageMessage(imageFilterParams,
                    "One of the following versions doesn't match between the images: activated parcel versions, " +
                            "cloudera manager version, runtime version."));
            case CM_UPGRADE_NOT_PERMITTED -> new ImageFilterResult(List.of(), getCantUpgradeToImageMessage(imageFilterParams,
                    String.format("Can't upgrade Cloudera Manager from %s gbn: %s to %s gbn: %s.",
                            imageFilterParams.getCurrentImage().getPackageVersion(ImagePackageVersion.CM),
                            imageFilterParams.getCurrentImage().getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER),
                            candidateImage.getPackageVersion(ImagePackageVersion.CM),
                            candidateImage.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER))));
            case STACK_UPGRADE_NOT_PERMITTED -> new ImageFilterResult(List.of(), getCantUpgradeToImageMessage(imageFilterParams,
                    String.format("Can't upgrade Cloudera Manager from %s gbn: %s to %s gbn: %s.",
                            imageFilterParams.getCurrentImage().getPackageVersion(ImagePackageVersion.STACK),
                            imageFilterParams.getCurrentImage().getPackageVersion(ImagePackageVersion.CDH_BUILD_NUMBER),
                            candidateImage.getPackageVersion(ImagePackageVersion.STACK),
                            candidateImage.getPackageVersion(ImagePackageVersion.CDH_BUILD_NUMBER))));
        };
    }

    private CmAndStackVersionMatchResult isUpgradePermitted(ImageFilterParams imageFilterParams, Image candidateImage) {
        if (imageFilterParams.isLockComponents()) {
            if (lockedComponentChecker.isUpgradePermitted(candidateImage, imageFilterParams.getStackRelatedParcels(), getCmBuildNumber(imageFilterParams))) {
                return CmAndStackVersionMatchResult.PERMITTED;
            } else {
                return CmAndStackVersionMatchResult.LOCKED_COMPONENTS_MISMATCH;
            }
        } else {
            if (!upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)) {
                return CmAndStackVersionMatchResult.CM_UPGRADE_NOT_PERMITTED;
            } else if (!upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage)) {
                return CmAndStackVersionMatchResult.STACK_UPGRADE_NOT_PERMITTED;
            } else {
                return CmAndStackVersionMatchResult.PERMITTED;
            }
        }
    }

    private String getCmBuildNumber(ImageFilterParams imageFilterParams) {
        return imageFilterParams.getCurrentImage().getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER);
    }

    enum CmAndStackVersionMatchResult {
        LOCKED_COMPONENTS_MISMATCH,
        CM_UPGRADE_NOT_PERMITTED,
        STACK_UPGRADE_NOT_PERMITTED,
        PERMITTED
    }
}