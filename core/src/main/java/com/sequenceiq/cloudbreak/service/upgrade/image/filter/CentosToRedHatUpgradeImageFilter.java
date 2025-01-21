package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentosToRedHatUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentosToRedHatUpgradeCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.common.model.OsType;

@Component
public class CentosToRedHatUpgradeImageFilter implements UpgradeImageFilter {

    private static final int ORDER_NUMBER = 11;

    @Inject
    private CentosToRedHatUpgradeAvailabilityService centOSToRedHatUpgradeAvailabilityService;

    @Inject
    private CentosToRedHatUpgradeCondition centosToRedHatUpgradeCondition;

    @Inject
    private CurrentImageUsageCondition currentImageUsageCondition;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        if (isCurrentImageUsingOs(imageFilterParams, RHEL8)) {
            return imageFilterResult;
        } else {
            List<Image> filteredImages = filterImages(imageFilterParams, imageFilterResult);
            return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
        }
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            return getCantUpgradeToImageMessage(imageFilterParams, "Can't upgrade to Red Hat Enterprise Linux.");
        } else {
            return "There are no eligible images to upgrade.";
        }
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private List<Image> filterImages(ImageFilterParams imageFilterParams, ImageFilterResult imageFilterResult) {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = imageFilterParams.getCurrentImage();
        List<Image> prefilteredImages = imageFilterResult.getImages().stream()
                .filter(image -> !isCentosToRedHadUpgrade(imageFilterParams, image)
                        || isCentOsToRedHatUpgradePossible(imageFilterParams, imageFilterResult, image, currentImage))
                .toList();
        List<Image> centOsImages = new ArrayList<>(prefilteredImages.stream()
                .filter(image -> isCentOSImage(image.getOs(), image.getOsType()))
                .toList());
        prefilteredImages.stream()
                .filter(image -> isRedHatImage(image.getOs(), image.getOsType()))
                .max(Comparator.comparing(Image::getCreated))
                .ifPresent(centOsImages::add);
        return prefilteredImages;
    }

    private boolean isCentosToRedHadUpgrade(ImageFilterParams imageFilterParams, Image image) {
        return centosToRedHatUpgradeCondition.isCentosToRedhatUpgrade(imageFilterParams.getStackId(), image);
    }

    private boolean isCurrentImageUsingOs(ImageFilterParams imageFilterParams, OsType osType) {
        return currentImageUsageCondition.isCurrentOsUsedOnInstances(imageFilterParams.getStackId(), osType.getOs());
    }

    private boolean isCentOsToRedHatUpgradePossible(ImageFilterParams imageFilterParams, ImageFilterResult imageFilterResult, Image image,
            com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return isCentOSToRedhatOsUpgradePermitted(imageFilterParams.getStackId(), currentImage, image, imageFilterParams.getStackRelatedParcels())
                || isCentOSImageAvailableWithSameVersion(imageFilterParams, imageFilterResult, image);
    }

    private static boolean isRedHatImage(String os, String osType) {
        return RHEL8.getOs().equalsIgnoreCase(os) && RHEL8.getOsType().equalsIgnoreCase(osType);
    }

    private static boolean isCentOSImage(String os, String osType) {
        return CENTOS7.getOs().equalsIgnoreCase(os) && CENTOS7.getOsType().equalsIgnoreCase(osType);
    }

    private boolean isCentOSImageAvailableWithSameVersion(ImageFilterParams imageFilterParams, ImageFilterResult imageFilterResult, Image image) {
        Set<String> stackRelatedParcels = imageFilterParams.getStackRelatedParcels().keySet();
        return hasTargetImage(imageFilterParams) ?
                centOSToRedHatUpgradeAvailabilityService.isHelperImageAvailable(imageFilterParams.getStackId(), imageFilterParams.getImageCatalogName(), image,
                        stackRelatedParcels) :
                centOSToRedHatUpgradeAvailabilityService.isHelperImageAvailable(imageFilterResult.getImages(), image, stackRelatedParcels);
    }

    private boolean isCentOSToRedhatOsUpgradePermitted(Long stackId, com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image image,
            Map<String, String> stackRelatedParcels) {
        return centOSToRedHatUpgradeAvailabilityService.isOsUpgradePermitted(stackId, currentImage, image, stackRelatedParcels);
    }
}