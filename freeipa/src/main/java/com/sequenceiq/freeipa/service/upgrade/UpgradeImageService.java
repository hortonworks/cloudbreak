package com.sequenceiq.freeipa.service.upgrade;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.FreeipaPlatformStringTransformer;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.image.PreferredOsService;

@Service
public class UpgradeImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeImageService.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    @Inject
    private ImageService imageService;

    @Inject
    private FreeipaPlatformStringTransformer platformStringTransformer;

    @Inject
    private PreferredOsService preferredOsService;

    public ImageInfoResponse selectImage(FreeIpaImageFilterSettings imageFilterParams) {
        Pair<ImageWrapper, String> imageWrapperAndName = imageService.fetchImageWrapperAndName(imageFilterParams);
        LOGGER.info("Selected image {} from request {}", imageWrapperAndName, imageFilterParams);
        return convertImageWrapperAndNameToImageInfoResponse(imageWrapperAndName);
    }

    private ImageInfoResponse convertImageWrapperAndNameToImageInfoResponse(Pair<ImageWrapper, String> imageWrapperAndName) {
        ImageInfoResponse imageInfoResponse = new ImageInfoResponse();
        imageInfoResponse.setImageName(imageWrapperAndName.getRight());
        imageInfoResponse.setDate(imageWrapperAndName.getLeft().getImage().getDate());
        imageInfoResponse.setCatalog(imageWrapperAndName.getLeft().getCatalogUrl());
        imageInfoResponse.setCatalogName(imageWrapperAndName.getLeft().getCatalogName());
        imageInfoResponse.setOs(imageWrapperAndName.getLeft().getImage().getOs());
        imageInfoResponse.setArchitecture(imageWrapperAndName.getLeft().getImage().getArchitecture());
        imageInfoResponse.setId(imageWrapperAndName.getLeft().getImage().getUuid());
        return imageInfoResponse;
    }

    public ImageInfoResponse fetchCurrentImage(Stack stack) {
        ImageEntity imageEntity = imageService.getByStackId(stack.getId());
        ImageInfoResponse imageInfoResponse = new ImageInfoResponse();
        imageInfoResponse.setImageName(imageEntity.getImageName());
        imageInfoResponse.setCatalog(imageEntity.getImageCatalogUrl());
        imageInfoResponse.setCatalogName(imageEntity.getImageCatalogName());
        imageInfoResponse.setOs(imageEntity.getOs());
        imageInfoResponse.setId(imageEntity.getImageId());
        imageInfoResponse.setArchitecture(imageEntity.getArchitecture());
        imageInfoResponse.setDate(imageEntity.getDate());
        LOGGER.info("Current image: {}", imageInfoResponse);
        return imageInfoResponse;
    }

    public List<ImageInfoResponse> findTargetImages(Stack stack, String catalog, ImageInfoResponse currentImage, Boolean allowMajorOsUpgrade) {
        return findTargetImages(stack, catalog, currentImage, allowMajorOsUpgrade, Map.of());
    }

    public List<ImageInfoResponse> findTargetImages(Stack stack, String catalog, ImageInfoResponse currentImage,
            Boolean allowMajorOsUpgrade, Map<String, String> tagFilters) {
        Optional<String> currentImageDate = getCurrentImageDate(stack, currentImage);
        List<Pair<ImageWrapper, String>> imagesWrapperAndName = imageService.fetchImagesWrapperAndName(stack, catalog, currentImage.getOs(),
                allowMajorOsUpgrade);
        List<ImageInfoResponse> targetImages = imagesWrapperAndName.stream()
                .filter(imageWrapperAndName -> currentImageDate.isPresent()
                        && isCandidateImageNewerThanCurrent(imageWrapperAndName.getLeft(), currentImageDate.get()))
                .filter(imageWrapperAndName -> !currentImage.getId().equals(imageWrapperAndName.getLeft().getImage().getUuid()))
                .filter(imageWrapperAndName -> filterBasedOnTags(tagFilters, imageWrapperAndName))
                .map(this::convertImageWrapperAndNameToImageInfoResponse)
                .collect(Collectors.toList());
        fetchDefaultOsImageIfNotPresentInTargets(stack, catalog, currentImage, targetImages).ifPresent(targetImages::add);
        return targetImages;
    }

    private Optional<ImageInfoResponse> fetchDefaultOsImageIfNotPresentInTargets(Stack stack, String catalog, ImageInfoResponse currentImage,
            List<ImageInfoResponse> targetImages) {
        try {
            OsType defaultOsType = OsType.getByOs(defaultOs);
            OsType currentOsType = OsType.getByOs(currentImage.getOs());
            if (defaultOsType != currentOsType
                    && defaultOsType.ordinal() > currentOsType.ordinal()
                    && targetImages.stream().noneMatch(img -> defaultOs.equalsIgnoreCase(img.getOs()))) {
                String platformString = platformStringTransformer.getPlatformString(stack);
                FreeIpaImageFilterSettings imageFilterSettings = new FreeIpaImageFilterSettings(null, catalog, defaultOs, defaultOs,
                        stack.getRegion(), platformString, false, stack.getArchitecture());
                Pair<ImageWrapper, String> rhel8ImageWrapper = imageService.fetchImageWrapperAndName(imageFilterSettings);
                return Optional.of(convertImageWrapperAndNameToImageInfoResponse(rhel8ImageWrapper));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't fetch extra default image as upgrade candidate.", e);
            return Optional.empty();
        }
    }

    private boolean isCandidateImageNewerThanCurrent(ImageWrapper candidate, String currentImageDate) {
        try {
            SimpleDateFormat imageDateFormat = new SimpleDateFormat(DATE_FORMAT);
            Date candidateDate = imageDateFormat.parse(candidate.getImage().getDate());
            Date currentDate = imageDateFormat.parse(currentImageDate);
            return candidateDate.after(currentDate);
        } catch (ParseException e) {
            LOGGER.warn("Couldn't parse dates, return false. Current date: [{}], candidate date: [{}]", currentImageDate, candidate.getImage().getDate(), e);
            return false;
        }
    }

    private boolean filterBasedOnTags(Map<String, String> tagFilters, Pair<ImageWrapper, String> imageWrapperAndName) {
        Map<String, String> tags = imageWrapperAndName.getLeft().getImage().getTags();
        for (Map.Entry<String, String> tagFilter : tagFilters.entrySet()) {
            if (tags.containsKey(tagFilter.getKey()) && !tagFilter.getValue().equals(tags.get(tagFilter.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private Optional<String> getCurrentImageDate(Stack stack, ImageInfoResponse currentImage) {
        if (StringUtils.isNotBlank(currentImage.getDate())) {
            LOGGER.debug("Current image has a date field filled with {}", currentImage.getDate());
            return Optional.of(currentImage.getDate());
        } else {
            return getCurrentImageDateFromCatalog(stack, currentImage);
        }
    }

    private Optional<String> getCurrentImageDateFromCatalog(Stack stack, ImageInfoResponse currentImage) {
        try {
            String catalog = Optional.ofNullable(currentImage.getCatalog()).orElse(currentImage.getCatalogName());
            LOGGER.debug("Current image date field is empty. Using catalog [{}] to get image date", catalog);
            FreeIpaImageFilterSettings freeIpaImageFilterSettings = createFreeIpaImageFilterSettings(stack, currentImage, catalog);
            ImageWrapper image = imageService.getImage(freeIpaImageFilterSettings);
            LOGGER.debug("Image date from catalog: {}", image.getImage().getDate());
            return Optional.ofNullable(image.getImage().getDate());
        } catch (ImageNotFoundException e) {
            LOGGER.warn("Image not found, returning empty date", e);
            return Optional.empty();
        }
    }

    private FreeIpaImageFilterSettings createFreeIpaImageFilterSettings(Stack stack, ImageInfoResponse currentImage, String catalog) {
        return new FreeIpaImageFilterSettings(
                currentImage.getId(),
                catalog,
                currentImage.getOs(),
                preferredOsService.getPreferredOs(currentImage.getOs()),
                stack.getRegion(),
                stack.getCloudPlatform().toLowerCase(Locale.ROOT),
                false,
                stack.getArchitecture(),
                Map.of()
        );
    }
}
