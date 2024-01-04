package com.sequenceiq.cloudbreak.service.image;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterUpgradeTargetImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeTargetImageService.class);

    private static final String TARGET_IMAGE = "TARGET_IMAGE";

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackImageService stackImageService;

    @Inject
    private ImageConverter imageConverter;

    public void saveImage(Long stackId, StatedImage targetImage) {
        Optional<Component> existingTargetImage = findTargetImageComponent(stackId);
        if (existingTargetImage.isPresent() && isTheSameImage(targetImage, existingTargetImage.get())) {
            LOGGER.debug("The target image {} is already present for stack {}", targetImage.getImage().getUuid(), stackId);
        } else {
            existingTargetImage.ifPresent(this::removeOldTargetImage);
            Component targetImageComponent = createTargetImageComponent(stackId, targetImage);
            LOGGER.debug("Saving target image component for cluster upgrade. {}", targetImageComponent);
            componentConfigProviderService.store(targetImageComponent);
        }
    }

    public Optional<Image> findTargetImage(Long stackId) {
        Optional<Component> targetImageComponent = findTargetImageComponent(stackId);
        return targetImageComponent.map(image -> imageConverter.convertJsonToImage(image.getAttributes()));
    }

    private void removeOldTargetImage(Component existingTargetImage) {
        LOGGER.debug("Deleting previous target image component {}", existingTargetImage);
        componentConfigProviderService.deleteComponents(Collections.singleton(existingTargetImage));
    }

    private Optional<Component> findTargetImageComponent(Long stackId) {
        return stackImageService.findImageComponentByName(stackId, TARGET_IMAGE);
    }

    private boolean isTheSameImage(StatedImage targetImage, Component existingTargetImage) {
        Image image = imageConverter.convertJsonToImage(existingTargetImage.getAttributes());
        return Objects.equals(image.getImageId(), targetImage.getImage().getUuid()) &&
                Objects.equals(image.getImageCatalogName(), targetImage.getImageCatalogName()) &&
                Objects.equals(image.getImageCatalogUrl(), targetImage.getImageCatalogUrl());
    }

    private Component createTargetImageComponent(Long stackId, StatedImage targetImage) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        Image imageModel = stackImageService.getImageModelFromStatedImage(stack, getCurrentImage(stack.getId()), targetImage);
        Stack stackReference = stackDtoService.getStackReferenceById(stackId);
        return new Component(ComponentType.IMAGE, TARGET_IMAGE, new Json(imageModel), stackReference);
    }

    private Image getCurrentImage(Long stackId) {
        try {
            return stackImageService.getCurrentImage(stackId);
        } catch (CloudbreakImageNotFoundException e) {
            String errorMessage = "Image not found in the database for this cluster.";
            LOGGER.error(errorMessage, e);
            throw new CloudbreakServiceException(errorMessage, e);
        }
    }
}
