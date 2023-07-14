package com.sequenceiq.freeipa.service.image;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

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

    @Inject
    private PreferredOsService preferredOsService;

    @Inject
    private SupportedOsService supportedOsService;

    @Override
    public Optional<ImageWrapper> getImage(FreeIpaImageFilterSettings imageFilterParams) {
        FreeIpaImageFilterSettings imageFilterSettings = populateImageFilterSettings(imageFilterParams);
        ImageCatalog cachedImageCatalog = imageCatalogProvider.getImageCatalog(imageFilterSettings.catalog());
        return findImageForAppVersion(imageFilterSettings, cachedImageCatalog)
                .or(() -> retryAfterEvictingCache(imageFilterSettings))
                .map(i -> new ImageWrapper(i, imageFilterSettings.catalog(), null));
    }

    @Override
    public List<ImageWrapper> getImages(FreeIpaImageFilterSettings freeIpaImageFilterSettings) {
        FreeIpaImageFilterSettings imageFilterSettings = populateImageFilterSettings(freeIpaImageFilterSettings);
        ImageCatalog cachedImageCatalog = imageCatalogProvider.getImageCatalog(imageFilterSettings.catalog());
        List<Image> compatibleImages = findImages(cachedImageCatalog.getImages().getFreeipaImages(), imageFilterSettings);
        List<String> imagesInVersions = filterFreeIpaVersionsByAppVersion(cachedImageCatalog.getVersions().getFreeIpaVersions()).stream()
                .map(FreeIpaVersions::getImageIds)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        LOGGER.debug("Compatible images: {} " + System.lineSeparator() + "Images in versions: {}", compatibleImages, imagesInVersions);
        return compatibleImages.stream()
                .filter(image -> imagesInVersions.contains(image.getUuid()))
                .map(image -> new ImageWrapper(image, imageFilterSettings.catalog(), null))
                .collect(Collectors.toList());
    }

    private FreeIpaImageFilterSettings populateImageFilterSettings(FreeIpaImageFilterSettings imageFilterSettings) {
        return new FreeIpaImageFilterSettings(imageFilterSettings.currentImageId(),
                StringUtils.isNotBlank(imageFilterSettings.catalog()) ? imageFilterSettings.catalog() : defaultCatalogUrl,
                imageFilterSettings.os(),
                imageFilterSettings.region(), imageFilterSettings.platform(), imageFilterSettings.allowMajorOsUpgrade());
    }

    private Optional<Image> findImageForAppVersion(FreeIpaImageFilterSettings freeIpaImageFilterSettings, ImageCatalog catalog) {
        List<FreeIpaVersions> versions = filterFreeIpaVersionsByAppVersion(catalog.getVersions().getFreeIpaVersions());
        List<Image> compatibleImages = findImages(catalog.getImages().getFreeipaImages(), freeIpaImageFilterSettings);
        LOGGER.debug("[{}] compatible images found, by the following parameters: imageId: {}, imageOs: {}, region: {}, platform: {}",
                compatibleImages.size(), freeIpaImageFilterSettings.currentImageId(), freeIpaImageFilterSettings.os(), freeIpaImageFilterSettings.region(),
                freeIpaImageFilterSettings.region());

        return findImageInDefaults(versions, compatibleImages)
                .or(() -> findImageByApplicationVersion(versions, compatibleImages))
                .or(() -> findMostRecentImage(compatibleImages));
    }

    private List<Image> findImages(List<Image> images, FreeIpaImageFilterSettings imageFilterSettings) {
        if (StringUtils.isNotBlank(imageFilterSettings.currentImageId())) {
            return images.stream()
                    .filter(img -> freeIpaImageFilter.filterPlatformAndRegion(imageFilterSettings, img))
                    .filter(img -> supportedOsService.isSupported(img.getOs()))
                    //It's not clear why we check the provider image reference (eg. the AMI in case of AWS) as imageId here.
                    //For safety and backward compatibility reasons the check remains here but should be checked if it really needed.
                    .filter(img -> hasSameUuid(imageFilterSettings.currentImageId(), img) || isMatchingImageIdInRegion(imageFilterSettings, img))
                    .collect(Collectors.toList());
        } else {
            return freeIpaImageFilter.filterImages(images, imageFilterSettings);
        }
    }

    private Boolean isMatchingImageIdInRegion(FreeIpaImageFilterSettings imageFilterSettings, Image img) {
        return Optional.ofNullable(img.getImageSetsByProvider().get(imageFilterSettings.platform()).get(imageFilterSettings.region()))
                .map(reg -> reg.equalsIgnoreCase(imageFilterSettings.currentImageId()))
                .orElse(false);
    }

    private boolean hasSameUuid(String imageId, Image img) {
        return img.getUuid().equalsIgnoreCase(imageId);
    }

    private Optional<Image> retryAfterEvictingCache(FreeIpaImageFilterSettings imageFilterSettings) {
        LOGGER.debug("Image not found with the parameters: imageId: {}, imageOs: {}, region: {}, platform: {}", imageFilterSettings.currentImageId(),
                imageFilterSettings.os(), imageFilterSettings.region(), imageFilterSettings.platform());
        LOGGER.debug("Evicting image catalog cache to retry.");
        imageCatalogProvider.evictImageCatalogCache(imageFilterSettings.catalog());
        ImageCatalog renewedImageCatalog = imageCatalogProvider.getImageCatalog(imageFilterSettings.catalog());
        return findImageForAppVersion(imageFilterSettings, renewedImageCatalog);
    }

    private Optional<Image> findMostRecentImage(List<Image> compatibleImages) {
        LOGGER.debug("Not found any image compatible with the application version. Falling back to the most recent image.");
        return compatibleImages.stream()
                .max(newestImageWithPreferredOs());
    }

    private Optional<Image> findImageByApplicationVersion(List<FreeIpaVersions> versions, List<Image> compatibleImages) {
        LOGGER.debug("Default image not found. Attempt to find an image, compatible with the application version.");
        return findImage(versions, compatibleImages, FreeIpaVersions::getImageIds);
    }

    private Optional<Image> findImageInDefaults(List<FreeIpaVersions> versions, List<Image> compatibleImages) {
        LOGGER.debug("Attempt to find a default image to use.");
        return findImage(versions, compatibleImages, FreeIpaVersions::getDefaults);
    }

    private Optional<Image> findImage(List<FreeIpaVersions> freeIpaVersions, List<Image> images, Function<FreeIpaVersions, List<String>> memberFunction) {
        List<String> imageIds = freeIpaVersions.stream().map(memberFunction).flatMap(Collection::stream).toList();
        return images.stream()
                .filter(image -> imageIds.contains(image.getUuid()))
                .max(newestImageWithPreferredOs());
    }

    private Comparator<Image> newestImageWithPreferredOs() {
        String preferredOs = preferredOsService.getPreferredOs();
        return Comparator.<Image, Integer>comparing(img -> preferredOs.equalsIgnoreCase(img.getOs()) ? 1 : 0)
                .thenComparing(Image::getDate);
    }

    private List<FreeIpaVersions> filterFreeIpaVersionsByAppVersion(List<FreeIpaVersions> freeIpaVersions) {
        List<FreeIpaVersions> exactFreeIpaVersionsMatches = freeIpaVersions.stream().filter(toExactVersionMatch()).collect(Collectors.toList());
        if (!exactFreeIpaVersionsMatches.isEmpty()) {
            LOGGER.debug("Exact version match found in image catalog for app version: {}", freeIpaVersion);
            return exactFreeIpaVersionsMatches;
        }
        List<FreeIpaVersions> prefixFreeIpaVersions = freeIpaVersions.stream().filter(toPrefixVersionMatch()).collect(Collectors.toList());
        if (!prefixFreeIpaVersions.isEmpty()) {
            LOGGER.debug("Prefix version match found in image catalog for app version: {}", freeIpaVersion);
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
