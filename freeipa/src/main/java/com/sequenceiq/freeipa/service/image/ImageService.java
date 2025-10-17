package com.sequenceiq.freeipa.service.image;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Images;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.converter.image.ImageToImageEntityConverter;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.image.change.action.ImageRevisionReaderService;
import com.sequenceiq.freeipa.repository.ImageRepository;

@Service
public class ImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    private static final String DEFAULT_REGION = "default";

    @Inject
    private ImageToImageEntityConverter imageConverter;

    @Inject
    private ImageRepository imageRepository;

    @Inject
    private ImageProviderFactory imageProviderFactory;

    @Inject
    private ImageRevisionReaderService imageRevisionReaderService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private Clock clock;

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    @Inject
    private ImageConverter imageEntityToImageConverter;

    @Inject
    private FreeipaPlatformStringTransformer platformStringTransformer;

    @Inject
    private PreferredOsService preferredOsService;

    public ImageEntity create(Stack stack, Pair<ImageWrapper, String> imageWrapperAndNamePair) {
        ImageEntity imageEntity = createImageEntity(stack, imageWrapperAndNamePair);
        return imageRepository.save(imageEntity);
    }

    private ImageEntity createImageEntity(Stack stack, Pair<ImageWrapper, String> imageWrapperAndNamePair) {
        ImageWrapper imageWrapper = imageWrapperAndNamePair.getLeft();
        ImageEntity imageEntity = imageConverter.convert(stack.getAccountId(), imageWrapper.getImage());
        imageEntity.setStack(stack);
        imageEntity.setImageName(imageWrapperAndNamePair.getRight());
        imageEntity.setAccountId(stack.getAccountId());
        imageEntity.setImageCatalogUrl(imageWrapper.getCatalogUrl());
        imageEntity.setImageCatalogName(imageWrapper.getCatalogName());
        imageEntity.setArchitecture(imageWrapper.getImage().getArchitecture());
        return imageEntity;
    }

    public ImageEntity changeImage(Stack stack, ImageSettingsRequest imageRequest) {
        LOGGER.info("Change image using request: {}", imageRequest);
        Pair<ImageWrapper, String> imageWrapperAndNamePair = fetchImageWrapperAndName(stack, imageRequest);
        ImageEntity imageEntity = updateImageWithNewValues(stack, imageWrapperAndNamePair.getLeft(), imageWrapperAndNamePair.getRight());
        LOGGER.info("New image entity: {}", imageEntity);
        return imageRepository.save(imageEntity);
    }

    public Pair<ImageWrapper, String> fetchImageWrapperAndName(FreeIpaImageFilterSettings imageFilterSettings) {
        String region = imageFilterSettings.region();
        String platformString = imageFilterSettings.platform();
        ImageWrapper imageWrapper = getImage(imageFilterSettings);
        String imageName = determineImageName(platformString, region, imageWrapper.getImage());
        LOGGER.info("Selected VM image for CloudPlatform '{}' and region '{}' is: {} from: {} image catalog with '{}' catalog name",
                platformString, region, imageName, imageWrapper.getCatalogUrl(), imageWrapper.getCatalogName());
        return Pair.of(imageWrapper, imageName);
    }

    public List<Pair<ImageWrapper, String>> fetchImagesWrapperAndName(Stack stack, String catalog, String currentImageOs, Boolean allowMajorOsUpgrade) {
        String region = stack.getRegion();
        String platformString = platformStringTransformer.getPlatformString(stack);
        List<ImageWrapper> imageWrappers = getImages(new FreeIpaImageFilterSettings(null, catalog, currentImageOs, null, stack.getRegion(), platformString,
                Boolean.TRUE.equals(allowMajorOsUpgrade), stack.getArchitecture()));
        LOGGER.debug("Images found: {}", imageWrappers);
        return imageWrappers.stream().map(imgw -> Pair.of(imgw, determineImageName(platformString, region, imgw.getImage()))).collect(Collectors.toList());
    }

    public Pair<ImageWrapper, String> fetchImageWrapperAndName(Stack stack, ImageSettingsRequest imageRequest) {
        FreeIpaImageFilterSettings imageFilterSettings = new FreeIpaImageFilterSettings(
                imageRequest.getId(),
                imageRequest.getCatalog(),
                null,
                StringUtils.isNotBlank(imageRequest.getOs()) ? imageRequest.getOs() : preferredOsService.getPreferredOs(imageRequest.getOs()),
                stack.getRegion(),
                platformStringTransformer.getPlatformString(stack),
                false,
                stack.getArchitecture());
        return fetchImageWrapperAndName(imageFilterSettings);
    }

    private ImageEntity updateImageWithNewValues(Stack stack, ImageWrapper imageWrapper, String imageName) {
        ImageEntity imageEntity = imageRepository.getByStack(stack);
        imageEntity.setImageName(imageName);
        imageEntity.setAccountId(stack.getAccountId());
        Image image = imageWrapper.getImage();
        imageEntity.setImageId(image.getUuid());
        imageEntity.setImageCatalogUrl(imageWrapper.getCatalogUrl());
        imageEntity.setImageCatalogName(imageWrapper.getCatalogName());
        imageEntity.setDate(image.getDate());
        imageEntity.setLdapAgentVersion(imageConverter.extractLdapAgentVersion(image));
        imageEntity.setSourceImage(imageConverter.extractSourceImage(image));
        imageEntity.setImdsVersion(imageConverter.extractImdsVersion(image));
        imageEntity.setSaltVersion(imageConverter.extractSaltVersion(image));
        imageEntity.setOs(imageWrapper.getImage().getOs());
        imageEntity.setOsType(imageWrapper.getImage().getOsType());
        return imageEntity;
    }

    public void revertImageToRevision(String accountId, Long imageEntityId, Number revision) {
        ImageEntity originalImageEntity = imageRevisionReaderService.find(imageEntityId, revision);
        LOGGER.info("Reverting to revision [{}] using {}", revision, originalImageEntity);
        ImageEntity imageEntity = imageRepository.findById(imageEntityId).get();
        imageEntity.setImageName(originalImageEntity.getImageName());
        imageEntity.setOs(originalImageEntity.getOs());
        imageEntity.setOsType(originalImageEntity.getOsType());
        imageEntity.setAccountId(accountId);
        imageEntity.setImageId(originalImageEntity.getImageId());
        imageEntity.setImageCatalogName(originalImageEntity.getImageCatalogName());
        imageEntity.setImageCatalogUrl(originalImageEntity.getImageCatalogUrl());
        imageEntity.setDate(originalImageEntity.getDate());
        imageEntity.setLdapAgentVersion(originalImageEntity.getLdapAgentVersion());
        imageEntity.setSourceImage(originalImageEntity.getSourceImage());
        imageEntity.setImdsVersion(originalImageEntity.getImdsVersion());
        imageEntity.setSaltVersion(originalImageEntity.getSaltVersion());
        imageEntity.setGatewayUserdata(originalImageEntity.getGatewayUserdata());
        imageRepository.save(imageEntity);
        LOGGER.info("Image reverted");
    }

    public ImageEntity getByStack(Stack stack) {
        return imageRepository.getByStack(stack);
    }

    public ImageEntity getByStackId(Long stackId) {
        return imageRepository.getByStackId(stackId);
    }

    public ImageEntity save(ImageEntity imageEntity) {
        return imageRepository.save(imageEntity);
    }

    public ImageWrapper getImage(FreeIpaImageFilterSettings imageFilterParams) {
        return imageProviderFactory.getImageProvider(imageFilterParams.catalog())
                .getImage(imageFilterParams)
                .orElseThrow(() -> throwImageNotFoundException(imageFilterParams.region(), imageFilterParams.currentImageId(),
                        Optional.ofNullable(imageFilterParams.targetOs()).orElse(defaultOs)));
    }

    private List<ImageWrapper> getImages(FreeIpaImageFilterSettings imageFilterParams) {
        return imageProviderFactory.getImageProvider(imageFilterParams.catalog()).getImages(imageFilterParams);
    }

    public String determineImageName(String platformString, String region, Image imgFromCatalog) {
        Optional<Map<String, String>> imagesForPlatform = findStringKeyWithEqualsIgnoreCase(platformString, imgFromCatalog.getImageSetsByProvider());
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            return selectImageByRegionPreferDefault(platformString, region, imgFromCatalog, imagesByRegion);
        } else {
            String msg = String.format("The selected image: '%s' doesn't contain virtual machine image for the selected platform: '%s'.",
                    imgFromCatalog, platformString);
            throw new ImageNotFoundException(msg);
        }
    }

    public String determineImageNameByRegion(String platformString, String region, Image imgFromCatalog) {
        Optional<Map<String, String>> imagesForPlatform = findStringKeyWithEqualsIgnoreCase(platformString, imgFromCatalog.getImageSetsByProvider());
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            return selectImageByRegion(platformString, region, imgFromCatalog, imagesByRegion);
        } else {
            String msg = String.format("The selected image: '%s' doesn't contain virtual machine image for the selected platform: '%s'.",
                    imgFromCatalog, platformString);
            throw new ImageNotFoundException(msg);
        }
    }

    private String selectImageByRegionPreferDefault(String platform, String region, Image imgFromCatalog, Map<String, String> imagesByRegion) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (CloudPlatform.AZURE.name().equalsIgnoreCase(platform) && entitlementService.azureMarketplaceImagesEnabled(accountId)) {
            return selectAzureMarketplaceImage(region, imagesByRegion, platform, accountId);
        } else {
            LOGGER.debug("Preferred region is region: {}. Platform: {}", region, platform);
            return findStringKeyWithEqualsIgnoreCase(region, imagesByRegion)
                    .or(() -> {
                        LOGGER.debug("Not found image with region: {}. Attempt to find with 'default' region. Platform: {}",
                                region, platform);
                        return findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion);
                    })
                    .orElseThrow(() -> new ImageNotFoundException(
                            String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                                    imgFromCatalog, platform, region)));
        }
    }

    private String selectAzureMarketplaceImage(String region, Map<String, String> imagesByRegion, String platform, String accountId)
            throws ImageNotFoundException {
        LOGGER.debug("Preferred region is 'default'. Platform: {}, Azure Marketplace images enabled.", platform);
        return findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion)
                .or(() -> supplyAlternativeImageWhenEntitlementAllows(region, imagesByRegion, accountId))
                .orElseThrow(() -> new ImageNotFoundException(
                        String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                                imagesByRegion, platform, region)));
    }

    private Optional<String> supplyAlternativeImageWhenEntitlementAllows(String region, Map<String, String> imagesByRegion, String accountId) {
        if (entitlementService.azureOnlyMarketplaceImagesEnabled(accountId)) {
            LOGGER.debug("No Azure Marketplace images found. Only Azure Marketplace images are allowed, skipping search for alternative images.");
            return Optional.empty();
        } else {
            LOGGER.debug("Searching for alternative Azure images in region: {}", region);
            return findStringKeyWithEqualsIgnoreCase(region, imagesByRegion);
        }
    }

    private String selectImageByRegion(String platformString, String region, Image imgFromCatalog, Map<String, String> imagesByRegion) {
        return findStringKeyWithEqualsIgnoreCase(region, imagesByRegion)
                .orElseThrow(() -> new ImageNotFoundException(
                        String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                                imgFromCatalog, platformString, region)));
    }

    private <T> Optional<T> findStringKeyWithEqualsIgnoreCase(String key, Map<String, T> map) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private ImageNotFoundException throwImageNotFoundException(String region, String imageId, String imageOs) {
        LOGGER.warn("Image not found in refreshed image catalog, by parameters: imageid: {}, region: {}, imageOs: {}", imageId, region, imageOs);
        String message = String.format("Could not find any image with id: '%s' in region '%s' with OS '%s'.", imageId, region, imageOs);
        return new ImageNotFoundException(message);
    }

    public ImageCatalog generateImageCatalogForStack(Stack stack) {
        final Image image = getImageForStack(stack);
        final Images images = new Images(List.of(copyImageWithAdvertisedFlag(image)));

        return new ImageCatalog(images, null);
    }

    public Image getImageForStack(Stack stack) {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettingsFromImageEntity(stack);
        ImageWrapper imageWrapper = getImage(imageFilterSettings);

        return imageWrapper.getImage();
    }

    public FreeIpaImageFilterSettings createImageFilterSettingsFromImageEntity(Stack stack) {
        ImageEntity imageEntity = getByStack(stack);
        return new FreeIpaImageFilterSettings(imageEntity.getImageId(),
                Objects.requireNonNullElse(imageEntity.getImageCatalogName(), imageEntity.getImageCatalogUrl()), imageEntity.getOs(), imageEntity.getOs(),
                stack.getRegion(), platformStringTransformer.getPlatformString(stack), false, stack.getArchitecture());
    }

    private Image copyImageWithAdvertisedFlag(Image source) {
        return new Image(
                source.getCreated(),
                source.getDate(),
                source.getDescription(),
                source.getOs(),
                source.getUuid(),
                source.getImageSetsByProvider(),
                source.getOsType(),
                source.getPackageVersions(),
                true,
                source.getArchitecture(),
                source.getTags()
        );
    }

    public List<ImageEntity> getImagesOfAliveStacks(Integer thresholdInDays) {
        final LocalDateTime thresholdDate = clock.getCurrentLocalDateTime()
                .minusDays(Optional.ofNullable(thresholdInDays).orElse(0));
        final long thresholdTimestamp = Timestamp.valueOf(thresholdDate).getTime();
        return imageRepository.findImagesOfAliveStacks(thresholdTimestamp);
    }

    public com.sequenceiq.cloudbreak.cloud.model.Image getCloudImageByStackId(Long stackId) {
        ImageEntity imageEntity = getByStackId(stackId);
        return imageEntityToImageConverter.convert(imageEntity);
    }
}
