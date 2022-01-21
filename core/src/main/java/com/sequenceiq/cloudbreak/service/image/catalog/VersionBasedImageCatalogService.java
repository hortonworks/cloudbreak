package com.sequenceiq.cloudbreak.service.image.catalog;

import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.CloudbreakVersionListProvider;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogVersionFilter;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.PrefixMatchImages;
import com.sequenceiq.cloudbreak.service.image.PrefixMatcherService;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

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

    @Inject
    private RawImageProvider rawImageProvider;

    @Inject
    private CloudbreakVersionListProvider cloudbreakVersionListProvider;

    @Override
    public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        return StringUtils.isNotEmpty(imageFilter.getCbVersion())
                ? versionBasedImageProvider.getImages(imageCatalogV3, imageFilter)
                : rawImageProvider.getImages(imageCatalogV3, imageFilter);
    }

    @Override
    public void validate(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        validateVersion(imageCatalogV3);
        validateUuids(imageCatalogV3);
    }

    @Override
    public ImageFilterResult getImageFilterResult(CloudbreakImageCatalogV3 imageCatalogV3) {
        Set<String> imageIds = getImageIds(imageCatalogV3);
        LOGGER.debug("{} image id(s) found for Cloudbreak version: {}", imageIds.size(), cbVersion);
        String message = String.format("%d image id(s) found for Cloudbreak version: %s", imageIds.size(), cbVersion);
        List<Image> freeipaImages = imageCatalogV3.getImages().getFreeIpaImages();
        if (!freeipaImages.isEmpty()) {
            List<Image> images = freeipaImages.stream().filter(image -> imageIds.contains(image.getUuid())).collect(Collectors.toList());
            return new ImageFilterResult(images, message);
        } else {
            List<Image> cdhImages = imageCatalogV3.getImages().getCdhImages();
            List<Image> images = cdhImages.stream().filter(image -> imageIds.contains(image.getUuid())).collect(Collectors.toList());
            return new ImageFilterResult(images, message);
        }
    }

    private void validateVersion(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        if (imageCatalogV3.getVersions() == null || cloudbreakVersionListProvider.getVersions(imageCatalogV3).isEmpty()) {
            throw new CloudbreakImageCatalogException("Cloudbreak versions cannot be NULL");
        }
    }

    private void validateUuids(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        Stream<String> baseUuids = imageCatalogV3.getImages().getBaseImages().stream().map(Image::getUuid);
        Stream<String> cdhUuids = imageCatalogV3.getImages().getCdhImages().stream().map(Image::getUuid);
        Stream<String> freeipaUuids = imageCatalogV3.getImages().getFreeIpaImages().stream().map(Image::getUuid);
        Stream<String> uuidStream = Stream.of(baseUuids, cdhUuids, freeipaUuids).reduce(Stream::concat).orElseGet(Stream::empty);
        List<String> uuidList = uuidStream.collect(Collectors.toList());
        List<String> orphanUuids = cloudbreakVersionListProvider.getVersions(imageCatalogV3).stream().flatMap(cbv -> cbv.getImageIds().stream()).
                filter(imageId -> !uuidList.contains(imageId)).collect(Collectors.toList());
        if (!orphanUuids.isEmpty()) {
            throw new CloudbreakImageCatalogException(String.format("Images with ids: %s is not present in cdh-images block",
                    StringUtils.join(orphanUuids, ",")));
        }
    }

    private Set<String> getImageIds(CloudbreakImageCatalogV3 imageCatalogV3) {
        Set<String> imageIds = new HashSet<>();
        List<CloudbreakVersion> cbVersionsFromImageCatalog = cloudbreakVersionListProvider.getVersions(imageCatalogV3);
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
