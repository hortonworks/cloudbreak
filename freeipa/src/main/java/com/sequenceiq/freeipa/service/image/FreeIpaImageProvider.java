package com.sequenceiq.freeipa.service.image;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
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

    @Value("${info.app.version:}")
    private String freeIpaVersion;

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
        List<String> imagesInVersions = filterFreeIpaVersionsByAppVersion(cachedImageCatalog.getVersions().getFreeIpaVersions())
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
        List<FreeIpaVersions> versions = filterFreeIpaVersionsByAppVersion(catalog.getVersions().getFreeIpaVersions());
        List<Image> compatibleImages = freeIpaImageFilter.filterImages(catalog.getImages().getFreeipaImages(), freeIpaImageFilterSettings);
        LOGGER.trace("[{}] compatible images found, by the following parameters: imageId: {}, imageOs: {}, region: {}, platform: {}, tagFilters: {}",
                compatibleImages.size(),
                freeIpaImageFilterSettings.currentImageId(),
                freeIpaImageFilterSettings.targetOs(),
                freeIpaImageFilterSettings.region(),
                freeIpaImageFilterSettings.platform(),
                freeIpaImageFilterSettings.tagFilters()
        );

        return findImageInDefaults(versions, compatibleImages)
                .or(() -> findImageByApplicationVersion(versions, compatibleImages))
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

    private Optional<Image> findImageByApplicationVersion(List<FreeIpaVersions> versions, List<Image> compatibleImages) {
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

    private List<FreeIpaVersions> filterFreeIpaVersionsByAppVersion(List<FreeIpaVersions> freeIpaVersions) {
        List<FreeIpaVersions> exactFreeIpaVersionsMatches = freeIpaVersions.stream()
                .filter(toExactVersionMatch())
                .toList();
        if (!exactFreeIpaVersionsMatches.isEmpty()) {
            LOGGER.trace("Exact version match found in image catalog for app version: {}", freeIpaVersion);
            return exactFreeIpaVersionsMatches;
        }
        List<FreeIpaVersions> prefixFreeIpaVersions = freeIpaVersions.stream()
                .filter(toPrefixVersionMatch())
                .collect(toList());
        if (!prefixFreeIpaVersions.isEmpty()) {
            LOGGER.trace("Prefix version match found in image catalog for app version: {}", freeIpaVersion);
            return prefixFreeIpaVersions;
        }

        LOGGER.warn("Not found matching version in image catalog. Falling back to most recent image.");
        return freeIpaVersions;
    }

    private Predicate<? super FreeIpaVersions> toPrefixVersionMatch() {
        return freeIpaVersions -> freeIpaVersions.getVersions().stream().anyMatch(
                version -> {
                    Optional<String> appVersionPrefix = extractVersionWithoutBuildTypeAndNumber(freeIpaVersion);
                    Optional<String> versionPrefix = extractVersionWithoutBuildTypeAndNumber(version);
                    return appVersionPrefix.isPresent() && appVersionPrefix.equals(versionPrefix);
                });
    }

    private Optional<String> extractVersionWithoutBuildTypeAndNumber(String version) {
        Matcher appVersionMatcher = VERSION_PATTERN.matcher(version);
        if (!appVersionMatcher.matches() || appVersionMatcher.groupCount() != 2) {
            return Optional.empty();
        }
        return Optional.of(appVersionMatcher.group(1));
    }

    private Predicate<? super FreeIpaVersions> toExactVersionMatch() {
        return freeIpaVersions -> freeIpaVersions.getVersions().contains(freeIpaVersion);
    }
}
