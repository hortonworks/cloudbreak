package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;

@Component
public class ClusterUpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeImageFilter.class);

    private static final String IGNORED_CM_VERSION = "7.x.0";

    @Inject
    private EntitlementDrivenPackageLocationFilter packageLocationFilter;

    @Inject
    private ImageCreationBasedFilter creationBasedFilter;

    @Inject
    private CmAndStackVersionFilter cmAndStackVersionFilter;

    @Inject
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @Inject
    private BlueprintUpgradeOptionValidator blueprintUpgradeOptionValidator;

    @Inject
    private EntitlementService entitlementService;

    public ImageFilterResult filter(String accountId, CloudbreakImageCatalogV3 imageCatalogV3, String cloudPlatform,
            ImageFilterParams imageFilterParams) {
        return isValidBlueprint(imageFilterParams, accountId) ? getImageFilterResult(imageCatalogV3, cloudPlatform, imageFilterParams)
                : createEmptyResult();
    }

    private ImageFilterResult getImageFilterResult(CloudbreakImageCatalogV3 imageCatalogV3, String cloudPlatform, ImageFilterParams imageFilterParams) {
        ImageFilterResult imagesForCbVersion = imageCatalogServiceProxy.getImageFilterResult(imageCatalogV3);
        List<Image> imageList = imagesForCbVersion.getAvailableImages().getCdhImages();
        if (CollectionUtils.isEmpty(imageList)) {
            return imagesForCbVersion;
        }
        LOGGER.debug("{} image(s) found for the given CB version", imageList.size());
        return filterImages(imageList, cloudPlatform, imageFilterParams);
    }

    private ImageFilterResult filterImages(List<Image> availableImages, String cloudPlatform, ImageFilterParams imageFilterParams) {
        Mutable<String> reason = new MutableObject<>();
        Image currentImage = imageFilterParams.getCurrentImage();
        List<Image> images = availableImages.stream()
                .filter(filterCurrentImage(currentImage, reason))
                .filter(filterNonCmImages(reason))
                .filter(filterIgnoredCmVersion(reason))
                .filter(creationBasedFilter.filterPreviousImages(currentImage, reason))
                .filter(cmAndStackVersionFilter.filterCmAndStackVersion(imageFilterParams, reason))
                .filter(validateCloudPlatform(cloudPlatform, reason))
                .filter(validateOsVersion(currentImage, reason))
                .filter(validatePackageLocation(imageFilterParams, currentImage, reason))
                .collect(Collectors.toList());

        return new ImageFilterResult(new Images(null, images, null, null), getReason(images, reason));
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
            return !image.getPackageVersion(ImagePackageVersion.CM).contains(IGNORED_CM_VERSION);
        };
    }

    private Predicate<Image> filterNonCmImages(Mutable<String> reason) {
        return image -> {
            reason.setValue("There are no eligible images to upgrade available with Cloudera Manager packages.");
            return isNotEmpty(image.getPackageVersion(ImagePackageVersion.CM));
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

    private Predicate<Image> validatePackageLocation(ImageFilterParams imageFilterParams, Image currentImage, Mutable<String> reason) {
        reason.setValue("There are no eligible images to upgrade because the location of the packages are not appropriate.");
        return packageLocationFilter.filterImage(currentImage, imageFilterParams);
    }

    private boolean isValidBlueprint(ImageFilterParams imageFilterParams, String accountId) {
        if (imageFilterParams.getStackType().equals(StackType.DATALAKE)) {
            boolean mediumDuty = imageFilterParams.getBlueprint().getName().contains("SDX Medium Duty");
            boolean canUpgradeMediumDuty = mediumDuty && entitlementService.mediumDutyUpgradeEnabled(accountId);
            return !mediumDuty || canUpgradeMediumDuty;
        } else {
            return blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams.getBlueprint());
        }
    }

    private boolean isOsVersionsMatch(Image currentImage, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentImage.getOs()) && newImage.getOsType().equalsIgnoreCase(currentImage.getOsType());
    }

    private ImageFilterResult createEmptyResult() {
        return new ImageFilterResult(new Images(null, Collections.emptyList(), null, null), "The upgrade is not allowed for this template.");
    }
}
