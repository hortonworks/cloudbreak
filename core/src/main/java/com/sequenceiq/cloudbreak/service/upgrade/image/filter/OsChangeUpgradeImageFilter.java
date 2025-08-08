package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeUtil;
import com.sequenceiq.common.model.OsType;

@Component
public class OsChangeUpgradeImageFilter implements UpgradeImageFilter {

    private static final int ORDER_NUMBER = 11;

    @Inject
    private OsChangeUtil osChangeUtil;

    @Inject
    private CurrentImageUsageCondition currentImageUsageCondition;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        Set<OsType> usedOperatingSystems = currentImageUsageCondition.getOSUsedByInstances(imageFilterParams.getStackId());
        List<Image> filteredImages = List.of();
        if (useOnly(RHEL9, usedOperatingSystems)) {
            return imageFilterResult;
        } else if (useOnly(RHEL8, usedOperatingSystems)) {
            filteredImages = filterImages(imageFilterParams, imageFilterResult.getImages(), RHEL8, RHEL9);
        } else if (useOnly(CENTOS7, usedOperatingSystems)) {
            filteredImages = filterImages(imageFilterParams, imageFilterResult.getImages(), CENTOS7, RHEL8);
        }
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
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

    private List<Image> filterImages(ImageFilterParams imageFilterParams, List<Image> images, OsType currentOsType, OsType nextOsType) {
        return images
                .stream()
                .filter(image -> currentOsType.matches(image.getOs(), image.getOsType()) ||
                        isOsChangeAllowed(imageFilterParams, image, images, currentOsType))
                .toList();
    }

    private boolean isOsChangeAllowed(ImageFilterParams imageFilterParams, Image imageCandidate, List<Image> images, OsType currentOsType) {
        return isOsUpgradePermitted(imageFilterParams, imageCandidate) ||
                isOSUpgradeImageAvailableWithSameVersion(imageFilterParams, images, imageCandidate, currentOsType);
    }

    private boolean isOSUpgradeImageAvailableWithSameVersion(ImageFilterParams imageFilterParams, List<Image> images, Image imageCandidate,
            OsType currentOsType) {
        Set<String> stackRelatedParcels = imageFilterParams.getStackRelatedParcels().keySet();
        return hasTargetImage(imageFilterParams) ?
                osChangeUtil.isHelperImageAvailable(imageFilterParams.getStackId(), imageFilterParams.getImageCatalogName(),
                        imageCandidate, stackRelatedParcels, currentOsType) :
                osChangeUtil.isHelperImageAvailable(images, imageCandidate, stackRelatedParcels, currentOsType);
    }

    private boolean isOsUpgradePermitted(ImageFilterParams imageFilterParams, Image imageCandidate) {
        return osChangeUtil.isOsUpgradePermitted(imageFilterParams.getStackId(), imageFilterParams.getCurrentImage(), imageCandidate,
                imageFilterParams.getStackRelatedParcels());
    }

    private boolean useOnly(OsType osType, Set<OsType> usedOS) {
        return usedOS.stream().allMatch(osType::equals);
    }
}