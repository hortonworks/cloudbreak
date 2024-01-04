package com.sequenceiq.cloudbreak.service.image.catalog;

import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.service.image.CloudbreakVersionListProvider;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogVersionFilter;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.ImageOsService;
import com.sequenceiq.cloudbreak.service.image.LatestDefaultImageUuidProvider;
import com.sequenceiq.cloudbreak.service.image.PrefixMatchImages;
import com.sequenceiq.cloudbreak.service.image.PrefixMatcherService;
import com.sequenceiq.cloudbreak.service.image.ProviderSpecificImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.common.model.ImageCatalogPlatform;

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

    @Inject
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @Inject
    private ImageOsService imageOsService;

    public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        Set<String> supportedVersions;

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
            supportedVersions = Collections.singleton(currentCbVersion);
        } else {
            LOGGER.debug("No image found with exact match for version {} Trying prefix matching", currentCbVersion);
            PrefixMatchImages prefixMatchImages = prefixMatcherService.prefixMatchForCBVersion(imageFilter.getCbVersion(), cloudbreakVersions);
            vMImageUUIDs.addAll(prefixMatchImages.getvMImageUUIDs());
            defaultVMImageUUIDs.addAll(prefixMatchImages.getDefaultVMImageUUIDs());
            supportedVersions = prefixMatchImages.getSupportedVersions();
        }
        LOGGER.info("The following images are matching for CB version ({}): {} ", currentCbVersion, vMImageUUIDs);

        List<Image> baseImages = filterImagesByPlatforms(
                imageFilter.getPlatforms(),
                imageCatalogV3.getImages().getBaseImages(),
                vMImageUUIDs,
                imageFilter.getClusterVersion());
        List<Image> cdhImages = filterImagesByPlatforms(
                imageFilter.getPlatforms(),
                imageCatalogV3.getImages().getCdhImages(),
                vMImageUUIDs,
                imageFilter.getClusterVersion());
        List<Image> freeipaImages = filterImagesByPlatforms(
                imageFilter.getPlatforms(),
                imageCatalogV3.getImages().getFreeIpaImages(),
                vMImageUUIDs,
                imageFilter.getClusterVersion());

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
        return statedImages(new Images(baseImages, cdhImages, freeipaImages, supportedVersions),
                imageFilter.getImageCatalog().getImageCatalogUrl(),
                imageFilter.getImageCatalog().getName());
    }

    private String getCBVersion(ImageFilter imageFilter, List<CloudbreakVersion> cloudbreakVersions) {
        return versionFilter.isVersionUnspecified(imageFilter.getCbVersion())
                ? versionFilter.latestCloudbreakVersion(cloudbreakVersions)
                : imageFilter.getCbVersion();
    }

    private List<Image> filterImagesByPlatforms(Collection<ImageCatalogPlatform> platforms, Collection<Image> images,
        Collection<String> vMImageUUIDs, String runtimeVersion) {
        List<Image> imageList = images.stream()
                .filter(isPlatformMatching(platforms, vMImageUUIDs))
                .filter(img -> isRuntimeVersionMatching(img, runtimeVersion))
                .filter(img -> imageOsService.isSupported(img.getOs()))
                .collect(toList());
        return providerSpecificImageFilter.filterImages(platforms, imageList);
    }

    private Predicate<Image> isPlatformMatching(Collection<ImageCatalogPlatform> platforms, Collection<String> vMImageUUIDs) {
        return img -> vMImageUUIDs.contains(img.getUuid())
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
