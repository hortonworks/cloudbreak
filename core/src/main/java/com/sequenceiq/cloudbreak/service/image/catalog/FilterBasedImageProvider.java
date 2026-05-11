package com.sequenceiq.cloudbreak.service.image.catalog;

import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.service.image.CloudbreakVersionListProvider;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.ImageOsService;
import com.sequenceiq.cloudbreak.service.image.LatestDefaultImageUuidProvider;
import com.sequenceiq.cloudbreak.service.image.ProviderSpecificImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class FilterBasedImageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterBasedImageProvider.class);

    @Inject
    private LatestDefaultImageUuidProvider latestDefaultImageUuidProvider;

    @Inject
    private CloudbreakVersionListProvider cloudbreakVersionListProvider;

    @Inject
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @Inject
    private ImageOsService imageOsService;

    public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        Images allImages = imageCatalogV3.getImages();
        List<String> imageUUIDs = cloudbreakVersionListProvider.getVersions(imageCatalogV3).stream()
                .flatMap(version -> version.getImageIds().stream()).collect(toList());

        List<Image> baseImages = filterImagesByPlatforms(imageFilter, allImages.getBaseImages(), imageUUIDs);
        List<Image> cdhImages = filterImagesByPlatforms(imageFilter, allImages.getCdhImages(), imageUUIDs);
        List<Image> freeipaImages = filterImagesByPlatforms(imageFilter, allImages.getFreeIpaImages(), imageUUIDs);

        if (freeipaImages.isEmpty()) {
            List<Image> defaultImages = cloudbreakVersionListProvider.getVersions(imageCatalogV3).stream()
                    .flatMap(version -> version.getDefaults().stream())
                    .map(imageId -> getImage(imageId, allImages))
                    .flatMap(Optional::stream)
                    .collect(toList());
            Collection<String> latestDefaultImageUuids = latestDefaultImageUuidProvider.getLatestDefaultImageUuids(imageFilter.getPlatforms(), defaultImages);
            LOGGER.info("Mark the following images as default, image ids: {}", latestDefaultImageUuids);
            Stream.concat(baseImages.stream(), cdhImages.stream())
                    .forEach(img -> img.setDefaultImage(latestDefaultImageUuids.contains(img.getUuid())));
        } else {
            LOGGER.info("Mark all FreeIPA images as default, image ids: {}", imageUUIDs);
            freeipaImages.forEach(img -> img.setDefaultImage(true));
        }

        if (!imageFilter.isBaseImageEnabled()) {
            baseImages.clear();
        }

        return statedImages(new Images(baseImages, cdhImages, freeipaImages),
                imageFilter.getImageCatalog().getImageCatalogUrl(),
                imageFilter.getImageCatalog().getName());
    }

    private List<Image> filterImagesByPlatforms(ImageFilter imageFilter, Collection<Image> images,
            Collection<String> vMImageUUIDs) {
        List<Image> imageList = images.stream()
                .filter(isPlatformMatching(imageFilter.getPlatforms(), vMImageUUIDs, imageFilter.isIncludingUnversionedImages()))
                .filter(img -> isRuntimeVersionMatching(img, imageFilter.getClusterVersion()))
                .filter(img -> imageOsService.isSupported(img.getOs()))
                .collect(toList());
        return providerSpecificImageFilter.filterImages(imageFilter.getPlatforms(), imageList);
    }

    private Predicate<Image> isPlatformMatching(Collection<ImageCatalogPlatform> platforms,
            Collection<String> vMImageUUIDs, boolean includeUnversionedImages) {
        return img -> (includeUnversionedImages || vMImageUUIDs.contains(img.getUuid()))
                && img.getImageSetsByProvider().keySet()
                .stream()
                .anyMatch(actualPlatform -> platformMatches(platforms, actualPlatform));
    }

    private boolean isRuntimeVersionMatching(Image image, String runtimeVersion) {
        return runtimeVersion == null || (image.getVersion() != null && image.getVersion().startsWith(runtimeVersion));
    }

    private boolean platformMatches(Collection<ImageCatalogPlatform> platforms, String p) {
        return platforms.stream().anyMatch(platform -> platform.nameToLowerCase().equalsIgnoreCase(p));
    }

    private Optional<? extends Image> getImage(String imageId, Images images) {
        return findFirstWithImageId(imageId, images.getFreeIpaImages())
                .or(() -> findFirstWithImageId(imageId, images.getBaseImages()))
                .or(() -> findFirstWithImageId(imageId, images.getCdhImages()));
    }

    private Optional<Image> findFirstWithImageId(String imageId, Collection<Image> images) {
        return images.stream()
                .filter(img -> img.getUuid().equals(imageId))
                .findFirst();
    }
}
