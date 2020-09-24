package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.service.image.VersionBasedImageFilter;

@Component
public class ClusterUpgradeImageFilter {

    public static final String CM_PACKAGE_KEY = "cm";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeImageFilter.class);

    private static final String IGNORED_CM_VERSION = "7.x.0";

    @Inject
    private VersionBasedImageFilter versionBasedImageFilter;

    @Inject
    private EntitlementDrivenPackageLocationFilter packageLocationFilter;

    @Inject
    private ImageCreationBasedFilter creationBasedFilter;

    @Inject
    private CmAndStackVersionFilter cmAndStackVersionFilter;

    public ImageFilterResult filter(List<Image> images, Versions versions, Image currentImage, String cloudPlatform, boolean lockComponents,
            Map<String, String> activatedParcels) {
        ImageFilterResult imagesForCbVersion = getImagesForCbVersion(versions, images);
        List<Image> imageList = imagesForCbVersion.getAvailableImages().getCdhImages();
        if (CollectionUtils.isEmpty(imageList)) {
            return imagesForCbVersion;
        }
        LOGGER.debug("{} image(s) found for the given CB version", imageList.size());
        return filterImages(imageList, currentImage, cloudPlatform, lockComponents, activatedParcels);
    }

    private ImageFilterResult getImagesForCbVersion(Versions supportedVersions, List<Image> availableImages) {
        return versionBasedImageFilter.getCdhImagesForCbVersion(supportedVersions, availableImages);
    }

    private ImageFilterResult filterImages(List<Image> availableImages, Image currentImage, String cloudPlatform,
            boolean lockComponents, Map<String, String> activatedParcels) {
        Mutable<String> reason = new MutableObject<>();
        List<Image> images = availableImages.stream()
                .filter(filterCurrentImage(currentImage, reason))
                .filter(filterNonCmImages(reason))
                .filter(filterIgnoredCmVersion(reason))
                .filter(creationBasedFilter.filterPreviousImages(currentImage, reason))
                .filter(cmAndStackVersionFilter.filterCmAndStackVersion(currentImage, lockComponents, activatedParcels, reason))
                .filter(validateCloudPlatform(cloudPlatform, reason))
                .filter(validateOsVersion(currentImage, reason))
                .filter(packageLocationFilter.filterImage(currentImage))
                .collect(Collectors.toList());

        return new ImageFilterResult(new Images(null, images, null), getReason(images, reason));
    }

    private String getReason(Collection<Image> images, Mutable<String> reason) {
        return images.isEmpty() ? reason.getValue() : null;
    }

    private Predicate<Image> filterCurrentImage(Image currentImage, Mutable<String> reason) {
        return image -> {
            reason.setValue("There are no newer compatible images available.");
            return !image.getUuid().equals(currentImage.getUuid());
        };
    }

    private Predicate<Image> filterIgnoredCmVersion(Mutable<String> reason) {
        return image -> {
            reason.setValue("There are no eligible images with supported Cloudera Manager or CDP version.");
            return !image.getPackageVersions().get(CM_PACKAGE_KEY).contains(IGNORED_CM_VERSION);
        };
    }

    private Predicate<Image> filterNonCmImages(Mutable<String> reason) {
        return image -> {
            reason.setValue("There are no eligible images to upgrade available with Cloudera Manager packages.");
            return isNotEmpty(image.getPackageVersions().get(CM_PACKAGE_KEY));
        };
    }

    private Predicate<Image> validateCloudPlatform(String cloudPlatform, Mutable<String> reason) {
        return image -> {
            reason.setValue(String.format("There are no eligible images to upgrade for %s cloud platform.", cloudPlatform));
            return image.getImageSetsByProvider().keySet().stream().anyMatch(key -> key.equalsIgnoreCase(cloudPlatform));
        };
    }

    private Predicate<Image> validateOsVersion(Image currentImage, Mutable<String> reason) {
        return image -> {
            reason.setValue("There are no eligible images to upgrade with the same OS version.");
            return isOsVersionsMatch(currentImage, image);
        };
    }

    private boolean isOsVersionsMatch(Image currentImage, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentImage.getOs()) && newImage.getOsType().equalsIgnoreCase(currentImage.getOsType());
    }
}
