package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class ImageCreationBasedFilter {

    public Predicate<Image> filterPreviousImages(Image currentImage, Mutable<String> reason) {
        return candidate -> {
            reason.setValue("There are no newer images available than " + currentImage.getDate() + ".");
            return isDifferentVersion(currentImage, candidate) || isNewerOrSameCreationImage(currentImage, candidate);
        };
    }

    private boolean isDifferentVersion(Image currentImage, Image candidate) {
        return Objects.nonNull(currentImage.getStackDetails())
                && StringUtils.isNotBlank(currentImage.getStackDetails().getVersion())
                && Objects.nonNull(candidate.getStackDetails())
                && StringUtils.isNotBlank(candidate.getStackDetails().getVersion())
                && !candidate.getStackDetails().getVersion().equalsIgnoreCase(currentImage.getStackDetails().getVersion());
    }

    private boolean isNewerOrSameCreationImage(Image currentImage, Image candidate) {
        return Objects.nonNull(candidate.getCreated())
                && Objects.nonNull(currentImage.getCreated())
                && candidate.getCreated() >= currentImage.getCreated();
    }
}
