package com.sequenceiq.cloudbreak.service.upgrade.image.filter;


import static com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult.EMPTY_REASON;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

public interface UpgradeImageFilter {

    ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams);

    String getMessage(ImageFilterParams imageFilterParams);

    Integer getFilterOrderNumber();

    default String getReason(List<Image> filteredImages, ImageFilterParams imageFilterParams) {
        return filteredImages.isEmpty() ? getMessage(imageFilterParams) : EMPTY_REASON;
    }

    default List<Image> filterImages(ImageFilterResult imageFilterResult, Predicate<Image> predicate) {
        return imageFilterResult.getImages().stream().filter(predicate).toList();
    }

    default String getCantUpgradeToImageMessage(ImageFilterParams imageFilterParams, String reason) {
        return String.format("Can't upgrade to '%s' image from '%s' image. %s", imageFilterParams.getTargetImageId(),
                imageFilterParams.getCurrentImage().getImageId(), reason);
    }

    default boolean hasTargetImage(ImageFilterParams imageFilterParams) {
        return Optional.ofNullable(imageFilterParams).map(ImageFilterParams::getTargetImageId).isPresent();
    }

    default void logNotEligibleImages(ImageFilterResult imageFilterResult, List<Image> filteredImages, Logger logger) {
        List<Image> notFilteredImages = new ArrayList<>(imageFilterResult.getImages());
        notFilteredImages.removeAll(filteredImages);
        if (!notFilteredImages.isEmpty()) {
            logger.debug("The following images are not eligible for upgrade regarding this filter: {}", notFilteredImages.stream()
                    .map(Image::getUuid).collect(Collectors.toList()));
        }
    }

}
