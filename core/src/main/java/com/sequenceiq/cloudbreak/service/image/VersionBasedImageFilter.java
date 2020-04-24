package com.sequenceiq.cloudbreak.service.image;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterResult;

@Component
public class VersionBasedImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionBasedImageFilter.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    public ImageFilterResult getCdhImagesForCbVersion(Versions versions, List<Image> availableImages) {
        List<String> imageIds = getImageIds(versions);
        LOGGER.debug("{} image id(s) found for Cloudbreak version: {}", imageIds.size(), cbVersion);
        String message = String.format("%d image id(s) found for Cloudbreak version: %s", imageIds.size(), cbVersion);
        List<Image> images = availableImages.stream()
                .filter(image -> imageIds.contains(image.getUuid()))
                .collect(Collectors.toList());
        return new ImageFilterResult(new Images(null, null, null, images, null), message);
    }

    private List<String> getImageIds(Versions versions) {
        return versions.getCloudbreakVersions().stream()
                .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(cbVersion))
                .map(CloudbreakVersion::getImageIds)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
