package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentOSToRedHatUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class CentOSToRedHatUpgradeImageFilter implements UpgradeImageFilter {

    public static final String REDHAT_8 = "redhat8";

    public static final String REDHAT_7 = "redhat7";

    public static final String CENTOS_7 = "centos7";

    private static final int ORDER_NUMBER = 11;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CentOSToRedHatUpgradeAvailabilityService centOSToRedHatUpgradeAvailabilityService;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        if (isRedHatImage(imageFilterParams.getCurrentImage().getOs(), imageFilterParams.getCurrentImage().getOsType())) {
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
        List<Image> preFilteredImages = imageFilterResult.getImages().stream()
                .filter(image -> !isCentOSToRedhatUpgrade(currentImage, image)
                        || isCentOsToRedHatUpgradePossible(imageFilterParams, imageFilterResult, image, currentImage))
                .toList();
        if (isCentOSImage(currentImage.getOs(), currentImage.getOsType())) {
            List<Image> centOsImages = new ArrayList<>(preFilteredImages.stream()
                    .filter(image -> isCentOSImage(image.getOs(), image.getOsType()))
                    .toList());
            preFilteredImages.stream()
                    .filter(image -> isRedHatImage(image.getOs(), image.getOsType()))
                    .max(Comparator.comparing(Image::getCreated))
                    .ifPresent(centOsImages::add);
        }
        return preFilteredImages;
    }

    private boolean isCentOsToRedHatUpgradePossible(ImageFilterParams imageFilterParams, ImageFilterResult imageFilterResult, Image image,
            com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        boolean rhel8ImagePreferred = entitlementService.isRhel8ImagePreferred(ThreadBasedUserCrnProvider.getAccountId());
        return rhel8ImagePreferred && isCentOSToRedhatOsUpgrade(currentImage, image, imageFilterParams.getStackRelatedParcels())
                || isCentOSImageAvailableWithSameVersion(imageFilterParams, imageFilterResult, image);
    }

    private boolean isCentOSImageAvailableWithSameVersion(ImageFilterParams imageFilterParams, ImageFilterResult imageFilterResult, Image image) {
        return centOSToRedHatUpgradeAvailabilityService.isHelperImageAvailable(imageFilterResult.getImages(), image,
                imageFilterParams.getStackRelatedParcels().keySet());
    }

    private boolean isCentOSToRedhatOsUpgrade(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image image, Map<String, String> stackRelatedParcels) {
        return centOSToRedHatUpgradeAvailabilityService.isOsUpgradePermitted(currentImage, image, stackRelatedParcels);
    }

    private static boolean isRedHatImage(String os, String osType) {
        return REDHAT_8.equalsIgnoreCase(os) && REDHAT_8.equalsIgnoreCase(osType);
    }

    private static boolean isCentOSImage(String os, String osType) {
        return CENTOS_7.equalsIgnoreCase(os) && REDHAT_7.equalsIgnoreCase(osType);
    }

    public static boolean isCentOSToRedhatUpgrade(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image newImage) {
        return currentImage.getOs().equalsIgnoreCase(CENTOS_7) &&
                currentImage.getOsType().equalsIgnoreCase(REDHAT_7) &&
                newImage.getOs().equalsIgnoreCase(REDHAT_8) &&
                newImage.getOsType().equalsIgnoreCase(REDHAT_8);
    }
}
