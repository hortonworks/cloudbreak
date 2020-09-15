package com.sequenceiq.cloudbreak.service.upgrade;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;
import com.sequenceiq.cloudbreak.service.image.VersionBasedImageFilter;

@Component
public class ClusterUpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeImageFilter.class);

    private static final String IGNORED_CM_VERSION = "7.x.0";

    private static final String CM_PACKAGE_KEY = "cm";

    private static final String STACK_PACKAGE_KEY = "stack";

    private static final String CDH_BUILD_NUMBER_KEY = "cdh-build-number";

    private static final String CM_BUILD_NUMBER_KEY = "cm-build-number";

    @Inject
    private VersionBasedImageFilter versionBasedImageFilter;

    @Inject
    private UpgradePermissionProvider upgradePermissionProvider;

    @Inject
    private PreWarmParcelParser preWarmParcelParser;

    @Inject
    private EntitlementDrivenPackageLocationFilter packageLocationFilter;

    @Inject
    private ImageCreationBasedFilter creationBasedFilter;

    ImageFilterResult filter(List<Image> images, Versions versions, Image currentImage, String cloudPlatform, boolean lockComponents,
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
        MutableObject<String> reason = new MutableObject<>();
        List<Image> images = availableImages.stream()
                .filter(filterCurrentImage(currentImage, reason))
                .filter(filterNonCmImages(reason))
                .filter(filterIgnoredCmVersion(reason))
                .filter(creationBasedFilter.filterPreviousImages(currentImage, reason))
                .filter(validateCmAndStackVersion(currentImage, lockComponents, activatedParcels, reason))
                .filter(validateCloudPlatform(cloudPlatform, reason))
                .filter(validateOsVersion(currentImage, reason))
                .filter(packageLocationFilter.filterImage(currentImage))
                .collect(Collectors.toList());

        return new ImageFilterResult(new Images(null, images, null), getReason(images, reason));
    }

    private String getReason(Collection<Image> images, MutableObject<String> reason) {
        return images.isEmpty() ? reason.getValue() : null;
    }

    private Predicate<Image> filterCurrentImage(Image currentImage, MutableObject<String> reason) {
        return image -> {
            reason.setValue("There are no newer compatible images available.");
            return !image.getUuid().equals(currentImage.getUuid());
        };
    }

    private Predicate<Image> filterIgnoredCmVersion(MutableObject<String> reason) {
        return image -> {
            reason.setValue("There are no eligible images with supported Cloudera Manager or CDP version.");
            return !image.getPackageVersions().get(CM_PACKAGE_KEY).contains(IGNORED_CM_VERSION);
        };
    }

    private Predicate<Image> filterNonCmImages(MutableObject<String> reason) {
        return image -> {
            reason.setValue("There are no eligible images to upgrade available with Cloudera Manager packages.");
            return isNotEmpty(image.getPackageVersions().get(CM_PACKAGE_KEY));
        };
    }

    private Predicate<Image> validateCmAndStackVersion(Image currentImage, boolean lockComponents, Map<String, String> activatedParcels,
            MutableObject<String> reason) {
        return image -> {
            boolean result = lockComponents ? (permitLockedComponentsUpgrade(image, activatedParcels))
                    : (permitCmAndStackUpgrade(currentImage, image, CM_PACKAGE_KEY, CM_BUILD_NUMBER_KEY)
                    && permitCmAndStackUpgrade(currentImage, image, STACK_PACKAGE_KEY, CDH_BUILD_NUMBER_KEY));

            if (lockComponents) {
                reason.setValue("There is at least one activated parcel for which we cannot find image with matching version. "
                        + "Activated parcel(s): " + activatedParcels);
            } else {
                reason.setValue("There is no proper Cloudera Manager or CDP version to upgrade.");
            }
            return result;
        };
    }

    // Only compares the versions of the activated parcels
    private boolean permitLockedComponentsUpgrade(Image image, Map<String, String> activatedParcels) {

        Map<String, String> prewarmedParcels = getParcels(image);
        String stackVersion = activatedParcels.get(StackType.CDH.name());

        boolean parcelsMatch = activatedParcels.entrySet()
                .stream()
                .filter(entry -> !StackType.CDH.name().equals(entry.getKey()))
                .allMatch(entry -> entry.getValue().equals(prewarmedParcels.get(entry.getKey())));

        boolean stackVersionMatches = StringUtils.isEmpty(stackVersion) || stackVersion.equals(
                Optional.ofNullable(image.getStackDetails())
                        .map(StackDetails::getRepo)
                        .map(StackRepoDetails::getStack)
                        .map(stackRepoDetails -> stackRepoDetails.get(com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION))
                        .orElse(stackVersion));
        return parcelsMatch && stackVersionMatches;
    }

    private Map<String, String> getParcels(Image image) {
        return image.getPreWarmParcels()
                .stream()
                .map(parcel -> preWarmParcelParser.parseProductFromParcel(parcel))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(ClouderaManagerProduct::getName, ClouderaManagerProduct::getVersion));
    }

    private boolean permitCmAndStackUpgrade(Image currentImage, Image image, String versionKey, String buildNumberKey) {
        return upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, image, versionKey, buildNumberKey);
    }

    private Predicate<Image> validateCloudPlatform(String cloudPlatform, MutableObject<String> reason) {
        return image -> {
            reason.setValue(String.format("There are no eligible images to upgrade for %s cloud platform.", cloudPlatform));
            return image.getImageSetsByProvider().keySet().stream().anyMatch(key -> key.equalsIgnoreCase(cloudPlatform));
        };
    }

    private Predicate<Image> validateOsVersion(Image currentImage, MutableObject<String> reason) {
        return image -> {
            reason.setValue("There are no eligible images to upgrade with the same OS version.");
            return isOsVersionsMatch(currentImage, image);
        };
    }

    private boolean isOsVersionsMatch(Image currentImage, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentImage.getOs()) && newImage.getOsType().equalsIgnoreCase(currentImage.getOsType());
    }
}
