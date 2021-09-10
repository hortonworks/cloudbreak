package com.sequenceiq.freeipa.service.upgrade;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;

@Service
public class UpgradeImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeImageService.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    @Inject
    private ImageService imageService;

    public ImageInfoResponse selectImage(Stack stack, ImageSettingsRequest imageSettingsRequest) {
        Pair<ImageWrapper, String> imageWrapperAndName = imageService.fetchImageWrapperAndName(stack, imageSettingsRequest);
        LOGGER.info("Selected image {} from request {}", imageWrapperAndName, imageSettingsRequest);
        return convertImageWrapperAndNameToImageInfoResponse(imageWrapperAndName);
    }

    private ImageInfoResponse convertImageWrapperAndNameToImageInfoResponse(Pair<ImageWrapper, String> imageWrapperAndName) {
        ImageInfoResponse imageInfoResponse = new ImageInfoResponse();
        imageInfoResponse.setImageName(imageWrapperAndName.getRight());
        imageInfoResponse.setDate(imageWrapperAndName.getLeft().getImage().getDate());
        imageInfoResponse.setCatalog(imageWrapperAndName.getLeft().getCatalogUrl());
        imageInfoResponse.setCatalogName(imageWrapperAndName.getLeft().getCatalogName());
        imageInfoResponse.setOs(imageWrapperAndName.getLeft().getImage().getOs());
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
        imageInfoResponse.setDate(imageEntity.getDate());
        LOGGER.info("Current image: {}", imageInfoResponse);
        return imageInfoResponse;
    }

    public List<ImageInfoResponse> findTargetImages(Stack stack, ImageSettingsRequest imageSettingsRequest, ImageInfoResponse currentImage) {
        Optional<String> currentImageDate = getCurrentImageDate(stack, currentImage);
        List<Pair<ImageWrapper, String>> imagesWrapperAndName = imageService.fetchImagesWrapperAndName(stack, imageSettingsRequest);
        return imagesWrapperAndName.stream()
                .filter(imageWrapperAndName -> currentImageDate.isPresent()
                        && isCandidateImageNewerThanCurrent(imageWrapperAndName.getLeft(), currentImageDate.get()))
                .filter(imageWrapperAndName -> !currentImage.getId().equals(imageWrapperAndName.getLeft().getImage().getUuid()))
                .map(this::convertImageWrapperAndNameToImageInfoResponse)
                .collect(Collectors.toList());
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
            ImageSettingsRequest imageSettings = new ImageSettingsRequest();
            imageSettings.setCatalog(catalog);
            imageSettings.setId(currentImage.getId());
            imageSettings.setOs(currentImage.getOs());
            ImageWrapper image = imageService.getImage(imageSettings, stack.getRegion(), stack.getCloudPlatform().toLowerCase());
            LOGGER.debug("Image date from catalog: {}", image.getImage().getDate());
            return Optional.ofNullable(image.getImage().getDate());
        } catch (ImageNotFoundException e) {
            LOGGER.warn("Image not found, returning empty date", e);
            return Optional.empty();
        }
    }
}
