package com.sequenceiq.freeipa.service.image;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.sequenceiq.freeipa.dto.ImageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.converter.image.ImageToImageEntityConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
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

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    public ImageEntity create(Stack stack, ImageSettingsRequest imageRequest) {
        String region = stack.getRegion();
        String platformString = stack.getCloudPlatform().toLowerCase();
        ImageWrapper imageWrapper = getImage(imageRequest, region, platformString);
        String imageName = determineImageName(platformString, region, imageWrapper.getImage());
        LOGGER.info("Selected VM image for CloudPlatform '{}' and region '{}' is: {} from: {} image catalog with '{}' catalog name",
                platformString, region, imageName, imageWrapper.getCatalogUrl(), imageWrapper.getCatalogName());

        ImageEntity imageEntity = imageConverter.convert(imageWrapper.getImage());
        imageEntity.setStack(stack);
        imageEntity.setImageName(imageName);
        imageEntity.setImageCatalogUrl(imageWrapper.getCatalogUrl());
        imageEntity.setImageCatalogName(imageWrapper.getCatalogName());

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

    public ImageWrapper getImage(ImageSettingsRequest imageSettings, String region, String platform) {
        return imageProviderFactory.getImageProvider(imageSettings.getCatalog())
                .getImage(imageSettings, region, platform)
                .orElseThrow(() -> throwImageNotFoundException(region, imageSettings.getId(), Optional.ofNullable(imageSettings.getOs()).orElse(defaultOs)));

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
}
