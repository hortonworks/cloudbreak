package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class CentOSToRedHatUpgradeImageFilter implements UpgradeImageFilter {

    public static final String REDHAT_8 = "redhat8";

    public static final String REDHAT_7 = "redhat7";

    public static final String CENTOS_7 = "centos7";

    public static final String VERSION_7_2_17 = "7.2.17";

    private static final Logger LOGGER = LoggerFactory.getLogger(CentOSToRedHatUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 11;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        String currentOs = imageFilterParams.getCurrentImage().getOs();
        String currentOsType = imageFilterParams.getCurrentImage().getOsType();
        List<Image> filteredImages = filterImages(imageFilterParams, imageFilterResult, currentOs, currentOsType);
        LOGGER.debug("After the filtering {} image left with proper OS {} and OS type {}.", filteredImages.size(), currentOs, currentOsType);
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return "There are no eligible images to upgrade.";
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private List<Image> filterImages(ImageFilterParams imageFilterParams, ImageFilterResult imageFilterResult, String currentOs, String currentOsType) {
        boolean rhel8ImagePreferred = entitlementService.isRhel8ImagePreferred(ThreadBasedUserCrnProvider.getAccountId());
        List<Image> preFilteredImages = imageFilterResult.getImages()
                .stream()
                .filter(image -> {
                    if (isCentOSImage(currentOs, currentOsType)) {
                        if (isRedHatImage(image.getOs(), image.getOsType())) {
                            return rhel8ImagePreferred && isCentOSToRedHatUpgradableVersion(imageFilterParams.getCurrentImage(), image);
                        }
                    }
                    return true;
                })
                .toList();
        if (isCentOSImage(currentOs, currentOsType)) {
            List<Image> result = new ArrayList<>(preFilteredImages.stream()
                    .filter(image -> isCentOSImage(image.getOs(), image.getOsType()))
                    .toList());
            preFilteredImages.stream()
                    .filter(image -> isRedHatImage(image.getOs(), image.getOsType()))
                    .max(Comparator.comparing(Image::getCreated))
                    .ifPresent(result::add);
            return result;
        } else {
            return preFilteredImages;
        }
    }

    public static boolean isCentOSToRedHatUpgradableVersion(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image image) {
        String currentStackVersion = currentImage.getPackageVersion(ImagePackageVersion.STACK);
        String imageStackVersion = image.getVersion();
        return isCentOSToRedhatUpgrade(currentImage.getOs(), currentImage.getOsType(), image)
                && VERSION_7_2_17.equals(currentStackVersion) && Objects.equals(currentStackVersion, imageStackVersion);
    }

    private static boolean isRedHatImage(String os, String osType) {
        return REDHAT_8.equalsIgnoreCase(os) && REDHAT_8.equalsIgnoreCase(osType);
    }

    private static boolean isCentOSImage(String os, String osType) {
        return CENTOS_7.equalsIgnoreCase(os) && REDHAT_7.equalsIgnoreCase(osType);
    }

    private static boolean isCentOSToRedhatUpgrade(String currentOs, String currentOsType, Image newImage) {
        return currentOs.equalsIgnoreCase(CENTOS_7) &&
                currentOsType.equalsIgnoreCase(REDHAT_7) &&
                newImage.getOs().equalsIgnoreCase(REDHAT_8) &&
                newImage.getOsType().equalsIgnoreCase(REDHAT_8);
    }
}
