package com.sequenceiq.cloudbreak.service.image;

import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class VersionBasedImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionBasedImageFilter.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private ImageCatalogVersionFilter versionFilter;

    @Inject
    private PrefixMatcherService prefixMatcherService;

    public ImageFilterResult getCdhImagesForCbVersion(Versions versions, List<Image> availableImages) {
        Set<String> imageIds = getImageIds(versions);
        LOGGER.debug("{} image id(s) found for Cloudbreak version: {}", imageIds.size(), cbVersion);
        String message = String.format("%d image id(s) found for Cloudbreak version: %s", imageIds.size(), cbVersion);
        List<Image> images = availableImages.stream()
                .filter(image -> imageIds.contains(image.getUuid()))
                .collect(Collectors.toList());
        return new ImageFilterResult(new Images(null, images, null), message);
    }

    private Set<String> getImageIds(Versions versions) {
        Set<String> imageIds = new HashSet<>();
        List<CloudbreakVersion> cbVersionsFromImageCatalog = versions.getCloudbreakVersions();
        String currentCbVersion = getCurrentCBVersion(cbVersionsFromImageCatalog);

        List<CloudbreakVersion> exactMatchedImages = getExtractMatchedImages(cbVersionsFromImageCatalog, currentCbVersion);
        if (!exactMatchedImages.isEmpty()) {
            for (CloudbreakVersion exactMatchedImg : exactMatchedImages) {
                imageIds.addAll(exactMatchedImg.getImageIds());
            }
        } else {
            LOGGER.debug("No image found with exact match for version {} Trying prefix matching", currentCbVersion);
            PrefixMatchImages prefixMatchImages = prefixMatcherService.prefixMatchForCBVersion(cbVersion, cbVersionsFromImageCatalog);
            imageIds.addAll(prefixMatchImages.getvMImageUUIDs());
        }
        return imageIds;
    }

    private List<CloudbreakVersion> getExtractMatchedImages(List<CloudbreakVersion> cloudbreakVersions, String currentCbVersion) {
        return cloudbreakVersions.stream()
                .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(currentCbVersion))
                .collect(toList());
    }

    private String getCurrentCBVersion(List<CloudbreakVersion> cbVersions) {
        return versionFilter.isVersionUnspecified(cbVersion)
                ? versionFilter.latestCloudbreakVersion(cbVersions)
                : cbVersion;
    }
}
