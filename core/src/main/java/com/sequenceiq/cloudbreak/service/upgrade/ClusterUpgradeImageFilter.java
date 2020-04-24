package com.sequenceiq.cloudbreak.service.upgrade;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.service.image.VersionBasedImageFilter;

@Component
public class ClusterUpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeImageFilter.class);

    private static final String IGNORED_CM_VERSION = "7.x.0";

    private static final String CM_PACKAGE_KEY = "cm";

    private static final String STACK_PACKAGE_KEY = "stack";

    private static final String SALT_PACKAGE_KEY = "salt";

    @Inject
    private VersionBasedImageFilter versionBasedImageFilter;

    @Inject
    private UpgradePermissionProvider upgradePermissionProvider;

    private String reason;

    ImageFilterResult filter(List<Image> images, Versions versions, Image currentImage, String cloudPlatform, boolean lockComponents) {
        ImageFilterResult imagesForCbVersion = getImagesForCbVersion(versions, images);
        List<Image> imageList = imagesForCbVersion.getAvailableImages().getCdhImages();
        if (CollectionUtils.isEmpty(imageList)) {
            return imagesForCbVersion;
        }
        LOGGER.debug("{} image(s) found for the given CB version", imageList.size());
        return filterImages(imageList, currentImage, cloudPlatform, lockComponents);
    }

    private ImageFilterResult getImagesForCbVersion(Versions supportedVersions, List<Image> availableImages) {
        return versionBasedImageFilter.getCdhImagesForCbVersion(supportedVersions, availableImages);
    }

    private ImageFilterResult filterImages(List<Image> availableImages, Image currentImage, String cloudPlatform, boolean lockComponents) {
        List<Image> images = availableImages.stream()
                .filter(filterCurrentImage(currentImage))
                .filter(filterNonCmImages())
                .filter(filterIgnoredCmVersion())
                .filter(validateCmAndStackVersion(currentImage, lockComponents))
                .filter(validateCloudPlatform(cloudPlatform))
                .filter(validateOsVersion(currentImage))
                .filter(validateSaltVersion(currentImage))
                .collect(Collectors.toList());

        return new ImageFilterResult(new Images(null, null, null, images, null), getReason(images));
    }

    private Predicate<Image> filterCurrentImage(Image currentImage) {
        return image -> {
            boolean result = !image.getUuid().equals(currentImage.getUuid());
            setReason(result, "There are no newer compatible images available.");
            return result;
        };
    }

    private Predicate<Image> filterIgnoredCmVersion() {
        return image -> {
            boolean result = !image.getPackageVersions().get(CM_PACKAGE_KEY).contains(IGNORED_CM_VERSION);
            setReason(result, "There are no eligible images with supported Cloudera Manager or CDP version.");
            return result;
        };
    }

    private Predicate<Image> filterNonCmImages() {
        return image -> {
            boolean result = isNotEmpty(image.getPackageVersions().get(CM_PACKAGE_KEY));
            setReason(result, "There are no eligible images to upgrade available with Cloudera Manager packages.");
            return result;
        };
    }

    private Predicate<Image> validateCmAndStackVersion(Image currentImage, boolean lockComponents) {
        return image -> {
            boolean result = lockComponents ? (permitLockedComponentsUpgrade(currentImage, image))
            : (permitCmAndStackUpgrade(currentImage, image, CM_PACKAGE_KEY)
                        || permitCmAndStackUpgrade(currentImage, image, STACK_PACKAGE_KEY));

            setReason(result, "There is no proper Cloudera Manager or CDP version to upgrade.");
            return result;
        };
    }

    private boolean permitLockedComponentsUpgrade(Image currentImage, Image image) {
        Map<String, String> currentImagePackageVersions = currentImage.getPackageVersions();
        Map<String, String> imagePackageVersions = image.getPackageVersions();
        return currentImagePackageVersions.equals(imagePackageVersions);
    }

    private boolean permitCmAndStackUpgrade(Image currentImage, Image image, String key) {
        return upgradePermissionProvider.permitCmAndStackUpgrade(currentImage.getPackageVersions().get(key),
                image.getPackageVersions().get(key));
    }

    private Predicate<Image> validateCloudPlatform(String cloudPlatform) {
        return image -> {
            boolean result = image.getImageSetsByProvider().keySet().stream().anyMatch(key -> key.equalsIgnoreCase(cloudPlatform));
            if (!result) {
                reason = String.format("There are no eligible images to upgrade for %s cloud platform.", cloudPlatform);
            }
            return result;
        };
    }

    private Predicate<Image> validateOsVersion(Image currentImage) {
        return image -> {
            boolean result = isOsVersionsMatch(currentImage, image);
            setReason(result, "There are no eligible images to upgrade with the same OS version.");
            return result;
        };
    }

    private boolean isOsVersionsMatch(Image currentImage, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentImage.getOs()) && newImage.getOsType().equalsIgnoreCase(currentImage.getOsType());
    }

    private Predicate<Image> validateSaltVersion(Image currentImage) {
        return image -> {
            boolean result = upgradePermissionProvider.permitSaltUpgrade(currentImage.getPackageVersions().get(SALT_PACKAGE_KEY),
                    image.getPackageVersions().get(SALT_PACKAGE_KEY));
            setReason(result, "There are no images with compatible Salt version.");
            return result;
        };
    }

    private String getReason(List<Image> images) {
        return images.isEmpty() ? reason : null;
    }

    private void setReason(boolean result, String reasonText) {
        if (!result) {
            reason = reasonText;
        }
    }
}
