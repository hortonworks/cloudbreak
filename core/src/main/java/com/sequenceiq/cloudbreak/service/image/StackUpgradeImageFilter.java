package com.sequenceiq.cloudbreak.service.image;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CompareLevel;
import com.sequenceiq.cloudbreak.cloud.CustomVersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;

@Component
public class StackUpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpgradeImageFilter.class);

    private static final String CM_PACKAGE_KEY = "cm";

    private static final String STACK_PACKAGE_KEY = "stack";

    private static final String CFM_PACKAGE_KEY = "cfm";

    private static final String CSP_PACKAGE_KEY = "csp";

    private static final String SALT_PACKAGE_KEY = "salt";

    @Inject
    private CustomVersionComparator customVersionComparator;

    @Inject
    private VersionBasedImageFilter versionBasedImageFilter;

    Images filter(List<Image> availableImages, Versions supportedVersions, com.sequenceiq.cloudbreak.cloud.model.Image currentImage, String cloudPlatform) {
        return new Images(null, null, null, getImages(availableImages, supportedVersions, currentImage, cloudPlatform), null);
    }

    private List<Image> getImages(List<Image> images, Versions versions, com.sequenceiq.cloudbreak.cloud.model.Image currentImage, String cloudPlatform) {
        List<Image> imagesForCbVersion = getImagesForCbVersion(versions, images);
        return filterImages(imagesForCbVersion, currentImage, cloudPlatform);
    }

    private List<Image> getImagesForCbVersion(Versions supportedVersions, List<Image> availableImages) {
        return versionBasedImageFilter.getCdhImagesForCbVersion(supportedVersions, availableImages);
    }

    private List<Image> filterImages(List<Image> availableImages, com.sequenceiq.cloudbreak.cloud.model.Image currentImage, String cloudPlatform) {
        return availableImages.stream()
                .filter(validateCmVersion(currentImage).or(validateStackVersion(currentImage)))
                .filter(validateCloudPlatform(cloudPlatform))
                .filter(validateOsVersion(currentImage))
                .filter(validateCfmVersion(currentImage))
                .filter(validateCspVersion(currentImage))
                .filter(validateSaltVersion(currentImage))
                .filter(filterCurrentImage(currentImage))
                .collect(Collectors.toList());
    }

    private Predicate<Image> validateOsVersion(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return image -> isOsVersionsMatch(currentImage, image);
    }

    private boolean isOsVersionsMatch(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentImage.getOs())
                && newImage.getOsType().equalsIgnoreCase(currentImage.getOsType());
    }

    private Predicate<Image> validateCmVersion(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return image -> compareVersion(currentImage.getPackageVersions().get(CM_PACKAGE_KEY), image.getPackageVersions().get(CM_PACKAGE_KEY));
    }

    private Predicate<Image> validateStackVersion(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return image -> compareVersion(currentImage.getPackageVersions().get(STACK_PACKAGE_KEY), image.getPackageVersions().get(STACK_PACKAGE_KEY));
    }

    private Predicate<Image> validateCloudPlatform(String cloudPlatform) {
        return image -> image.getImageSetsByProvider().keySet().stream().anyMatch(key -> key.equalsIgnoreCase(cloudPlatform));
    }

    private Predicate<Image> validateCfmVersion(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return image -> image.getPackageVersions().get(CFM_PACKAGE_KEY).equals(currentImage.getPackageVersions().get(CFM_PACKAGE_KEY));
    }

    private Predicate<Image> validateCspVersion(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return image -> image.getPackageVersions().get(CSP_PACKAGE_KEY).equals(currentImage.getPackageVersions().get(CSP_PACKAGE_KEY));
    }

    private Predicate<Image> validateSaltVersion(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return image -> image.getPackageVersions().get(SALT_PACKAGE_KEY).equals(currentImage.getPackageVersions().get(SALT_PACKAGE_KEY));
    }

    private Predicate<Image> filterCurrentImage(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return image -> !image.getUuid().equals(currentImage.getImageId());
    }

    private boolean compareVersion(String currentVersion, String newVersion) {
        boolean result;
        try {
            result = customVersionComparator.compare(currentVersion, newVersion, CompareLevel.MAINTENANCE) < 0;
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed to compare versions: %s %s", currentVersion, newVersion));
            result = false;
        }
        return result;
    }
}
