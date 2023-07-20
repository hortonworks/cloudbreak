package com.sequenceiq.cloudbreak.service.image.catalog;

import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.ImageOsService;
import com.sequenceiq.cloudbreak.service.image.LatestDefaultImageUuidProvider;
import com.sequenceiq.cloudbreak.service.image.ProviderSpecificImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class AdvertisedImageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvertisedImageProvider.class);

    @Inject
    private LatestDefaultImageUuidProvider latestDefaultImageUuidProvider;

    @Inject
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @Inject
    private ImageOsService imageOsService;

    public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        return statedImages(
                new Images(
                        getImages(getBaseImagesOrEmptyList(imageCatalogV3, imageFilter), imageFilter.getPlatforms()),
                        getImages(imageCatalogV3.getImages().getCdhImages(), imageFilter.getPlatforms()),
                        imageCatalogV3.getImages().getFreeIpaImages(),
                        singleton(imageFilter.getCbVersion())),
                imageFilter.getImageCatalog().getImageCatalogUrl(),
                imageFilter.getImageCatalog().getName());
    }

    private List<Image> getImages(List<Image> images, Set<ImageCatalogPlatform> platforms) {
        List<Image> result = images.stream()
                .filter(Image::isAdvertised)
                .filter(img -> img.getImageSetsByProvider().keySet().stream()
                        .anyMatch(p ->
                                platforms.stream().anyMatch(platform -> platform.nameToLowerCase().equalsIgnoreCase(p))))
                .filter(image -> imageOsService.isSupported(image.getOs()))
                .collect(toList());

        Collection<String> latestDefaultImageUuids = latestDefaultImageUuidProvider.getLatestDefaultImageUuids(platforms, result);
        result.forEach(image -> image.setDefaultImage(latestDefaultImageUuids.contains(image.getUuid())));
        return providerSpecificImageFilter.filterImages(platforms, result);
    }

    private List<Image> getBaseImagesOrEmptyList(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        return imageFilter.isBaseImageEnabled() ?
                getImages(imageCatalogV3.getImages().getBaseImages(), imageFilter.getPlatforms())
                : Collections.emptyList();
    }
}
