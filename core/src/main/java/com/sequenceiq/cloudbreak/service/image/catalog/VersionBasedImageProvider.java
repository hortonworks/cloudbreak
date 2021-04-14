package com.sequenceiq.cloudbreak.service.image.catalog;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.service.image.CloudbreakVersionListProvider;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogVersionFilter;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.LatestDefaultImageUuidProvider;
import com.sequenceiq.cloudbreak.service.image.PrefixMatchImages;
import com.sequenceiq.cloudbreak.service.image.PrefixMatcherService;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static java.util.stream.Collectors.toList;

@Component
public class VersionBasedImageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionBasedImageProvider.class);

    @Inject
    private ImageCatalogVersionFilter versionFilter;

    @Inject
    private PrefixMatcherService prefixMatcherService;

    @Inject
    private LatestDefaultImageUuidProvider latestDefaultImageUuidProvider;

    @Inject
    private CloudbreakVersionListProvider cloudbreakVersionListProvider;

    public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        Set<String> suppertedVersions;

        Set<String> vMImageUUIDs = new HashSet<>();
        Set<String> defaultVMImageUUIDs = new HashSet<>();
        String currentCbVersion;

        List<CloudbreakVersion> cloudbreakVersions = cloudbreakVersionListProvider.getVersions(imageCatalogV3);

        currentCbVersion = getCBVersion(imageFilter, cloudbreakVersions);

        List<CloudbreakVersion> exactMatchedImages = cloudbreakVersions.stream()
                .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(currentCbVersion)).collect(toList());

        if (!exactMatchedImages.isEmpty()) {
            for (CloudbreakVersion exactMatchedImg : exactMatchedImages) {
                vMImageUUIDs.addAll(exactMatchedImg.getImageIds());
                defaultVMImageUUIDs.addAll(exactMatchedImg.getDefaults());
            }
            suppertedVersions = Collections.singleton(currentCbVersion);
        } else {
            LOGGER.debug("No image found with exact match for version {} Trying prefix matching", currentCbVersion);
            PrefixMatchImages prefixMatchImages = prefixMatcherService.prefixMatchForCBVersion(imageFilter.getCbVersion(), cloudbreakVersions);
            vMImageUUIDs.addAll(prefixMatchImages.getvMImageUUIDs());
            defaultVMImageUUIDs.addAll(prefixMatchImages.getDefaultVMImageUUIDs());
            suppertedVersions = prefixMatchImages.getSupportedVersions();
        }
        LOGGER.info("The following images are matching for CB version ({}): {} ", currentCbVersion, vMImageUUIDs);

        List<Image> baseImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV3.getImages().getBaseImages(), vMImageUUIDs);
        List<Image> cdhImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV3.getImages().getCdhImages(), vMImageUUIDs);
        List<Image> freeipaImages = filterImagesByPlatforms(imageFilter.getPlatforms(), imageCatalogV3.getImages().getFreeIpaImages(), vMImageUUIDs);

        List<Image> defaultImages = defaultVMImageUUIDs.stream()
                .map(imageId -> getImage(imageId, imageCatalogV3.getImages()))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        Collection<String> latestDefaultImageUuids = latestDefaultImageUuidProvider.getLatestDefaultImageUuids(imageFilter.getPlatforms(), defaultImages);
        (!freeipaImages.isEmpty() ?
                freeipaImages.stream() :
                Stream.of(baseImages.stream(), cdhImages.stream()).reduce(Stream::concat).orElseGet(Stream::empty))
                .forEach(img -> img.setDefaultImage(latestDefaultImageUuids.contains(img.getUuid())));

        if (!imageFilter.isBaseImageEnabled()) {
            baseImages.clear();
        }
        return statedImages(new Images(baseImages, cdhImages, freeipaImages, suppertedVersions),
                imageFilter.getImageCatalog().getImageCatalogUrl(),
                imageFilter.getImageCatalog().getName());
    }

    private String getCBVersion(ImageFilter imageFilter, List<CloudbreakVersion> cloudbreakVersions) {
        return versionFilter.isVersionUnspecified(imageFilter.getCbVersion())
                ? versionFilter.latestCloudbreakVersion(cloudbreakVersions)
                : imageFilter.getCbVersion();
    }

    private List<Image> filterImagesByPlatforms(Collection<String> platforms, Collection<Image> images, Collection<String> vMImageUUIDs) {
        return images.stream()
                .filter(isPlatformMatching(platforms, vMImageUUIDs))
                .collect(toList());
    }

    private static Predicate<Image> isPlatformMatching(Collection<String> platforms, Collection<String> vMImageUUIDs) {
        return img -> vMImageUUIDs.contains(img.getUuid())
                && img.getImageSetsByProvider().keySet().stream().anyMatch(p -> platforms.stream().anyMatch(platform -> platform.equalsIgnoreCase(p)));
    }

    private Optional<? extends Image> getImage(String imageId, Images images) {
        Optional<? extends Image> image;
        if (!images.getFreeIpaImages().isEmpty()) {
            image = findFirstWithImageId(imageId, images.getFreeIpaImages());
        } else {
            image = findFirstWithImageId(imageId, images.getBaseImages());
            if (image.isEmpty()) {
                image = findFirstWithImageId(imageId, images.getCdhImages());
            }
        }
        return image;
    }

    private Optional<? extends Image> findFirstWithImageId(String imageId, Collection<? extends Image> images) {
        return images.stream()
                .filter(img -> img.getUuid().equals(imageId))
                .findFirst();
    }
}
