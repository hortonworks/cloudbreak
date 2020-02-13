package com.sequenceiq.cloudbreak.service.image;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;

@Component
public class VersionBasedImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionBasedImageFilter.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    public List<Image> getCdhImagesForCbVersion(Versions versions, List<Image> availableImages) {
        List<String> imageIds = getImageIds(versions);
        LOGGER.debug(String.format("%s image found for %s Cloudbreak version.", imageIds.size(), cbVersion));
        return availableImages.stream()
                .filter(image -> imageIds.contains(image.getUuid()))
                .collect(Collectors.toList());
    }

    private List<String> getImageIds(Versions versions) {
        return versions.getCloudbreakVersions().stream()
                .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(cbVersion))
                .map(CloudbreakVersion::getImageIds)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
