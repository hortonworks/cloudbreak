package com.sequenceiq.cloudbreak.service.image.catalog;

import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.ProviderSpecificImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class RawImageProvider {

    @Inject
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        Images catalogImages = imageCatalogV3.getImages();
        Set<ImageCatalogPlatform> platforms = imageFilter.getPlatforms();
        List<Image> baseImages = filterImagesByPlatforms(platforms, catalogImages.getBaseImages());
        List<Image> cdhImages = filterImagesByPlatforms(platforms, catalogImages.getCdhImages());
        List<Image> freeipaImages = filterImagesByPlatforms(platforms, catalogImages.getFreeIpaImages());

        return statedImages(
                new Images(baseImages, cdhImages, freeipaImages, catalogImages.getSupportedVersions()),
                imageFilter.getImageCatalog().getImageCatalogUrl(),
                imageFilter.getImageCatalog().getName());
    }

    List<Image> filterImagesByPlatforms(Collection<ImageCatalogPlatform> platforms, Collection<Image> images) {
        List<Image> imageList = images.stream()
                .filter(isPlatformMatching(platforms))
                .collect(toList());
        return providerSpecificImageFilter.filterImages(platforms, imageList);
    }

    private static Predicate<Image> isPlatformMatching(Collection<ImageCatalogPlatform> platforms) {
        return img -> img.getImageSetsByProvider().keySet()
                .stream()
                .anyMatch(p -> platforms.stream().anyMatch(platform -> platform.nameToLowerCase().equalsIgnoreCase(p)));
    }

}
