package com.sequenceiq.cloudbreak.service.image.catalog;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogVersionFilter;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.PrefixMatchImages;
import com.sequenceiq.cloudbreak.service.image.PrefixMatcherService;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
public class VersionBasedImageCatalogService implements ImageCatalogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionBasedImageCatalogService.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private ImageCatalogVersionFilter versionFilter;

    @Inject
    private PrefixMatcherService prefixMatcherService;

    @Inject
    private VersionBasedImageProvider versionBasedImageProvider;

    @Override
    public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        return versionBasedImageProvider.getImages(imageCatalogV3, imageFilter);
    }

    @Override
    public void validate(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        validateVersion(imageCatalogV3);
        validateUuids(imageCatalogV3);
    }

    @Override
    public ImageFilterResult getImageFilterResult(CloudbreakImageCatalogV3 imageCatalogV3) {
        Set<String> imageIds = getImageIds(imageCatalogV3.getVersions());
        LOGGER.debug("{} image id(s) found for Cloudbreak version: {}", imageIds.size(), cbVersion);
        String message = String.format("%d image id(s) found for Cloudbreak version: %s", imageIds.size(), cbVersion);
        List<Image> images = imageCatalogV3.getImages().getCdhImages().stream()
                .filter(image -> imageIds.contains(image.getUuid()))
                .collect(Collectors.toList());
        return new ImageFilterResult(new Images(null, images, null), message);
    }

    private void validateVersion(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        if (imageCatalogV3.getVersions() == null || imageCatalogV3.getVersions().getCloudbreakVersions().isEmpty()) {
            throw new CloudbreakImageCatalogException("Cloudbreak versions cannot be NULL");
        }
    }

    private void validateUuids(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        Stream<String> baseUuids = imageCatalogV3.getImages().getBaseImages().stream().map(Image::getUuid);
        Stream<String> cdhUuids = imageCatalogV3.getImages().getCdhImages().stream().map(Image::getUuid);
        Stream<String> uuidStream = Stream.of(baseUuids, cdhUuids).
                reduce(Stream::concat).
                orElseGet(Stream::empty);
        List<String> uuidList = uuidStream.collect(Collectors.toList());
        List<String> orphanUuids = imageCatalogV3.getVersions().getCloudbreakVersions().stream().flatMap(cbv -> cbv.getImageIds().stream()).
                filter(imageId -> !uuidList.contains(imageId)).collect(Collectors.toList());
        if (!orphanUuids.isEmpty()) {
            throw new CloudbreakImageCatalogException(String.format("Images with ids: %s is not present in cdh-images block",
                    StringUtils.join(orphanUuids, ",")));
        }
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
