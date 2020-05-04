package com.sequenceiq.freeipa.service.image;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.model.image.FreeIpaVersions;
import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.model.image.ImageCatalog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.converter.image.ImageToImageEntityConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.ImageRepository;

@Service
public class ImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    private static final String DEFAULT_REGION = "default";

    private static final Pattern VERSION_PATTERN = Pattern.compile("^([0-9]+\\.[0-9]+\\.[0-9]+-(dev\\.|rc\\.|[b]))[0-9]+$");

    @Inject
    private ImageToImageEntityConverter imageConverter;

    @Inject
    private ImageRepository imageRepository;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Value("${freeipa.image.catalog.url}")
    private String defaultCatalogUrl;

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    @Value("${info.app.version:}")
    private String freeIpaVersion;

    public ImageEntity create(Stack stack, ImageSettingsRequest imageRequest) {
        String region = stack.getRegion();
        String platformString = stack.getCloudPlatform().toLowerCase();
        com.sequenceiq.freeipa.api.model.image.Image imageCatalogImage = getImage(imageRequest, region, platformString);
        String imageName = determineImageName(platformString, region, imageCatalogImage);
        String catalogUrl = Objects.nonNull(imageRequest.getCatalog()) ? imageRequest.getCatalog() : defaultCatalogUrl;
        LOGGER.info("Selected VM image for CloudPlatform '{}' and region '{}' is: {} from: {} image catalog",
                platformString, region, imageName, catalogUrl);

        ImageEntity imageEntity = imageConverter.convert(imageCatalogImage);
        imageEntity.setStack(stack);
        imageEntity.setImageName(imageName);
        imageEntity.setImageCatalogUrl(catalogUrl);
        return imageRepository.save(imageEntity);
    }

    public ImageEntity getByStack(Stack stack) {
        return imageRepository.getByStack(stack);
    }

    public ImageEntity getByStackId(Long stackId) {
        return imageRepository.getByStackId(stackId);
    }

    public ImageEntity decorateImageWithUserDataForStack(Stack stack, String userdata) {
        ImageEntity imageEntity = getByStack(stack);
        imageEntity.setUserdata(userdata);
        return imageRepository.save(imageEntity);
    }

    public Image getImage(ImageSettingsRequest imageSettings, String region, String platform) {
        String imageId = imageSettings.getId();
        String catalogUrl = Objects.nonNull(imageSettings.getCatalog()) ? imageSettings.getCatalog() : defaultCatalogUrl;
        String imageOs = Objects.nonNull(imageSettings.getOs()) ? imageSettings.getOs() : defaultOs;

        ImageCatalog cachedImageCatalog = imageCatalogProvider.getImageCatalog(catalogUrl);
        return findImageForAppVersion(region, platform, imageId, imageOs, cachedImageCatalog)
                .or(() -> retryAfterEvictingCache(region, platform, imageId, catalogUrl, imageOs))
                .orElseThrow(() -> throwImageNotFoundException(region, imageId, imageOs));
    }

    private ImageNotFoundException throwImageNotFoundException(String region, String imageId, String imageOs) {
            LOGGER.warn("Image not found in refreshed image catalog, by parameters: imageid: {}, region: {}, imageOs: {}", imageId, region, imageOs);
            String message = String.format("Could not find any image with id: '%s' in region '%s' with OS '%s'.", imageId, region, imageOs);
            return new ImageNotFoundException(message);
    }

    private Optional<Image> retryAfterEvictingCache(String region, String platform, String imageId, String catalogUrl, String imageOs) {
            LOGGER.debug("Image not found with the parameters: imageId: {}, imageOs: {}, region: {}, platform: {}", imageId, imageOs, region, platform);
            LOGGER.debug("Evicting image catalog cache to retry.");
            imageCatalogProvider.evictImageCatalogCache(catalogUrl);
            ImageCatalog renewedImageCatalog = imageCatalogProvider.getImageCatalog(catalogUrl);
            return findImageForAppVersion(region, platform, imageId, imageOs, renewedImageCatalog);
    }

    private Optional<Image> findImageForAppVersion(String region, String platform, String imageId, String imageOs, ImageCatalog catalog) {
        List<FreeIpaVersions> versions = filterFreeIpaVersionsByAppVersion(catalog.getVersions().getFreeIpaVersions());
        List<Image> compatibleImages = findImage(imageId, imageOs, catalog.getImages().getFreeipaImages(), region, platform);
        LOGGER.debug("[{}] compatible images found, by the following parameters: imageId: {}, imageOs: {}, region: {}, platform: {}",
                compatibleImages.size(), imageId, imageOs, region, platform);

        return findImageInDefaults(versions, compatibleImages)
                .or(() -> findImageByApplicationVersion(versions, compatibleImages))
                .or(() -> findMostRecentImage(compatibleImages));
    }

    private Optional<Image> findMostRecentImage(List<Image> compatibleImages) {
            LOGGER.debug("Not found any image compatible with the application version. Falling back to the most recent image.");
            return compatibleImages.stream().max(Comparator.comparing(Image::getDate));
    }

    private Optional<Image> findImageByApplicationVersion(List<FreeIpaVersions> versions, List<Image> compatibleImages) {
            LOGGER.debug("Default image not found. Attempt to find an image, compatible with the application version.");
            return filterImages(versions, compatibleImages, FreeIpaVersions::getImageIds);
    }

    private Optional<Image> findImageInDefaults(List<FreeIpaVersions> versions, List<Image> compatibleImages) {
        LOGGER.debug("Attempt to find a default image to use.");
        return filterImages(versions, compatibleImages, FreeIpaVersions::getDefaults);
    }

    private Optional<Image> filterImages(List<FreeIpaVersions> freeIpaVersions, List<Image> images,
            Function<FreeIpaVersions, List<String>> memberFunction) {
        List<String> imageIds = freeIpaVersions.stream().map(memberFunction).flatMap(Collection::stream).collect(Collectors.toList());
        return images.stream().filter(image -> imageIds.contains(image.getUuid())).max(Comparator.comparing(Image::getDate));
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

    public String determineImageName(String platformString, String region, Image imgFromCatalog) {
        Optional<Map<String, String>> imagesForPlatform = findStringKeyWithEqualsIgnoreCase(platformString, imgFromCatalog.getImageSetsByProvider());
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            Optional<String> imageNameOpt = findStringKeyWithEqualsIgnoreCase(region, imagesByRegion);
            if (!imageNameOpt.isPresent()) {
                imageNameOpt = findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion);
            }
            if (imageNameOpt.isPresent()) {
                return imageNameOpt.get();
            }
            String msg = String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                    imgFromCatalog, platformString, region);
            throw new ImageNotFoundException(msg);
        }
        String msg = String.format("The selected image: '%s' doesn't contain virtual machine image for the selected platform: '%s'.",
                imgFromCatalog, platformString);
        throw new ImageNotFoundException(msg);
    }

    private List<Image> filterImages(List<Image> imageList, String os,
            String platform, String region) {
        Predicate<Image> predicate = img -> img.getOs().equalsIgnoreCase(os)
                && img.getImageSetsByProvider().containsKey(platform) && img.getImageSetsByProvider().get(platform).containsKey(region);
        Map<Boolean, List<Image>> partitionedImages =
                Optional.ofNullable(imageList).orElse(Collections.emptyList()).stream()
                        .collect(Collectors.partitioningBy(predicate));
        if (hasFiltered(partitionedImages)) {
            LOGGER.debug("Used filter for: | {} | Images filtered: {}",
                    os,
                    partitionedImages.get(false).stream().map(Image::toString).collect(Collectors.joining(", ")));
            return partitionedImages.get(true);
        } else {
            LOGGER.warn("No FreeIPA image found with OS {}, falling back to the latest available one if such exists!", os);
            return imageList;
        }
    }

    private boolean hasFiltered(Map<Boolean, List<Image>> partitioned) {
        return !partitioned.get(true).isEmpty();
    }

    private List<Image> findImage(String imageId, String imageOs,
            List<Image> images, String region, String platform) {
        if (Objects.nonNull(imageOs) && !imageOs.isEmpty()) {
            images = filterImages(images, imageOs, platform, region);
        }
        if (Objects.nonNull(imageId) && !imageId.isEmpty()) {
            return images.stream()
                    .filter(img -> img.getImageSetsByProvider().containsKey(platform) && img.getImageSetsByProvider().get(platform).containsKey(region))
                    .filter(img -> img.getImageSetsByProvider().get(platform).get(region).equalsIgnoreCase(imageId))
                    .collect(Collectors.toList());
        } else {
            return images.stream().filter(image -> image.getImageSetsByProvider().containsKey(platform))
                    .collect(Collectors.toList());
        }
    }

    private <T> Optional<T> findStringKeyWithEqualsIgnoreCase(String key, Map<String, T> map) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
