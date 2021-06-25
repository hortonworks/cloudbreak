package com.sequenceiq.freeipa.service.upgrade;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageService;

@Service
public class UpgradeImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeImageService.class);

    @Inject
    private ImageService imageService;

    public ImageInfoResponse selectImage(Stack stack, ImageSettingsRequest imageSettingsRequest) {
        Pair<ImageWrapper, String> imageWrapperAndName = imageService.fetchImageWrapperAndName(stack, imageSettingsRequest);
        LOGGER.info("Selected image {} from request {}", imageWrapperAndName, imageSettingsRequest);
        ImageInfoResponse imageInfoResponse = new ImageInfoResponse();
        imageInfoResponse.setImageName(imageWrapperAndName.getRight());
        imageInfoResponse.setDate(imageWrapperAndName.getLeft().getImage().getDate());
        imageInfoResponse.setCatalog(imageWrapperAndName.getLeft().getCatalogUrl());
        imageInfoResponse.setCatalogName(imageWrapperAndName.getLeft().getCatalogName());
        imageInfoResponse.setOs(imageWrapperAndName.getLeft().getImage().getOs());
        imageInfoResponse.setId(imageWrapperAndName.getLeft().getImage().getUuid());
        return imageInfoResponse;
    }

    public ImageInfoResponse currentImage(Stack stack) {
        ImageEntity imageEntity = imageService.getByStackId(stack.getId());
        ImageInfoResponse imageInfoResponse = new ImageInfoResponse();
        imageInfoResponse.setImageName(imageEntity.getImageName());
        imageInfoResponse.setCatalog(imageEntity.getImageCatalogUrl());
        imageInfoResponse.setCatalogName(imageEntity.getImageCatalogName());
        imageInfoResponse.setOs(imageEntity.getOs());
        imageInfoResponse.setId(imageEntity.getImageId());
        LOGGER.info("Current image: {}", imageInfoResponse);
        return imageInfoResponse;
    }
}
