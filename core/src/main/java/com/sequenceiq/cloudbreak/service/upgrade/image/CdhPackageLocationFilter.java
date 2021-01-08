package com.sequenceiq.cloudbreak.service.upgrade.image;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class CdhPackageLocationFilter implements PackageLocationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdhPackageLocationFilter.class);

    @Override
    public boolean filterImage(Image image, Image currentImage, ImageFilterParams imageFilterParams) {
        if (isRelevantFieldNull(image, currentImage)) {
            LOGGER.debug("Image or some part of it is null: {}", image);
            return false;
        } else {
            String repoUrl = image.getStackDetails().getRepo().getStack().getOrDefault(currentImage.getOsType(), "");
            LOGGER.debug("Matching URL: [{}]", repoUrl);
            return URL_PATTERN.matcher(repoUrl).find();
        }
    }

    private boolean isRelevantFieldNull(Image image, Image currentImage) {
        return image == null
                || image.getStackDetails() == null
                || image.getStackDetails().getRepo() == null
                || image.getStackDetails().getRepo().getStack() == null
                || currentImage == null
                || StringUtils.isBlank(currentImage.getOsType());
    }
}
