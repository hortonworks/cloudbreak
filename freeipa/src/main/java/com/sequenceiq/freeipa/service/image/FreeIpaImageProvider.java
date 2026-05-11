package com.sequenceiq.freeipa.service.image;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.FreeIpaVersions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.freeipa.dto.ImageWrapper;

@Service
public class FreeIpaImageProvider implements ImageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaImageProvider.class);

    private static final Pattern VERSION_PATTERN = Pattern.compile("^([0-9]+\\.[0-9]+\\.[0-9]+-(dev\\.|rc\\.|[b]))[0-9]+$");

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Value("${freeipa.image.catalog.url}")
    private String defaultCatalogUrl;

    @Inject
    private FreeIpaImageFilter freeIpaImageFilter;

    @Override
    public Optional<ImageWrapper> getImage(FreeIpaImageFilterSettings imageFilterParams) {
        FreeIpaImageFilterSettings imageFilterSettings = populateImageFilterSettings(imageFilterParams);
        ImageCatalog cachedImageCatalog = imageCatalogProvider.getImageCatalog(imageFilterSettings.catalog());
        return findImageForAppVersion(imageFilterSettings, cachedImageCatalog)
                .or(() -> retryAfterEvictingCache(imageFilterSettings))
                .map(i -> ImageWrapper.ofFreeipaImage(i, imageFilterSettings.catalog()));
    }

    public List<ImageWrapper> getImages(FreeIpaImageFilterSettings freeIpaImageFilterSettings) {
        FreeIpaImageFilterSettings imageFilterSettings = populateImageFilterSettings(freeIpaImageFilterSettings);
        ImageCatalog cachedImageCatalog = imageCatalogProvider.getImageCatalog(imageFilterSettings.catalog());
        List<Image> compatibleImages = freeIpaImageFilter.filterImages(cachedImageCatalog.getImages().getFreeipaImages(), imageFilterSettings);
        List<String> imagesInVersions = cachedImageCatalog.getVersions().getFreeIpaVersions()
                .stream()
                .map(FreeIpaVersions::getImageIds)
                .flatMap(Collection::stream)
                .distinct()
                .collect(toList());
        LOGGER.debug("Compatible images: {} " + System.lineSeparator() + "Images in versions: {}", compatibleImages, imagesInVersions);
        return compatibleImages.stream()
                .filter(image -> imagesInVersions.contains(image.getUuid()))
                .map(image -> ImageWrapper.ofFreeipaImage(image, imageFilterSettings.catalog()))
                .toList();
    }

    private FreeIpaImageFilterSettings populateImageFilterSettings(FreeIpaImageFilterSettings imageFilterSettings) {
        return new FreeIpaImageFilterSettings(imageFilterSettings.currentImageId(),
                StringUtils.isNotBlank(imageFilterSettings.catalog()) ? imageFilterSettings.catalog() : defaultCatalogUrl,
                imageFilterSettings.currentOs(), imageFilterSettings.targetOs(),
                imageFilterSettings.region(), imageFilterSettings.platform(), imageFilterSettings.allowMajorOsUpgrade(), imageFilterSettings.architecture(),
                imageFilterSettings.tagFilters());
    }

    private Optional<Image> findImageForAppVersion(FreeIpaImageFilterSettings freeIpaImageFilterSettings, ImageCatalog catalog) {
        List<Image> compatibleImages = freeIpaImageFilter.filterImages(catalog.getImages().getFreeipaImages(), freeIpaImageFilterSettings);
        LOGGER.trace("[{}] compatible images found, by the following parameters: imageId: {}, imageOs: {}, region: {}, platform: {}, tagFilters: {}",
                compatibleImages.size(),
                freeIpaImageFilterSettings.currentImageId(),
                freeIpaImageFilterSettings.targetOs(),
                freeIpaImageFilterSettings.region(),
                freeIpaImageFilterSettings.platform(),
                freeIpaImageFilterSettings.tagFilters()
        );

        List<FreeIpaVersions> freeIpaVersions = catalog.getVersions().getFreeIpaVersions();
        return findImageInDefaults(freeIpaVersions, compatibleImages)
                .or(() -> findNonDefaultImage(freeIpaVersions, compatibleImages))
                .or(() -> freeIpaImageFilter.findMostRecentImage(compatibleImages));
    }

    private Optional<Image> retryAfterEvictingCache(FreeIpaImageFilterSettings imageFilterSettings) {
        LOGGER.debug("Image not found with the parameters: imageId: {}, imageOs: {}, region: {}, platform: {}", imageFilterSettings.currentImageId(),
                imageFilterSettings.targetOs(), imageFilterSettings.region(), imageFilterSettings.platform());
        LOGGER.debug("Evicting image catalog cache to retry.");
        imageCatalogProvider.evictImageCatalogCache(imageFilterSettings.catalog());
        ImageCatalog renewedImageCatalog = imageCatalogProvider.getImageCatalog(imageFilterSettings.catalog());
        return findImageForAppVersion(imageFilterSettings, renewedImageCatalog);
    }

    private Optional<Image> findNonDefaultImage(List<FreeIpaVersions> versions, List<Image> compatibleImages) {
        LOGGER.debug("Default image not found. Attempt to find an image, compatible with the application version.");
        return findImage(versions, compatibleImages, FreeIpaVersions::getImageIds);
    }

    private Optional<Image> findImageInDefaults(List<FreeIpaVersions> versions, List<Image> compatibleImages) {
        LOGGER.trace("Attempt to find a default image to use.");
        return findImage(versions, compatibleImages, FreeIpaVersions::getDefaults);
    }

    private Optional<Image> findImage(List<FreeIpaVersions> freeIpaVersions, List<Image> images, Function<FreeIpaVersions, List<String>> memberFunction) {
        List<String> imageIds = freeIpaVersions.stream()
                .map(memberFunction)
                .flatMap(Collection::stream)
                .toList();
        return images.stream()
                .filter(image -> imageIds.contains(image.getUuid()))
                .max(freeIpaImageFilter.newestImageWithPreferredOs());
    }

}
