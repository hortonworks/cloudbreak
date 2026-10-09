package com.sequenceiq.cloudbreak.service.image;

import java.util.Collections;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

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

    @Inject
    private UserDataService userDataService;

    public void saveImage(Long stackId, ClusterUpgradeProperties properties) {
        Image targetImage = properties.toCloudImage();
        Optional<Component> existingTargetImage = findTargetImageComponent(stackId);
        if (existingTargetImage.isPresent()
                && targetImage.equals(imageConverter.convertJsonToImage(existingTargetImage.get().getAttributes()))) {
            LOGGER.debug("The target image {} is already present for stack {}", targetImage.getImageId(), stackId);
        } else {
            userDataService.makeSureUserDataIsMigrated(stackId);
            existingTargetImage.ifPresent(this::removeOldTargetImage);
            Component component = new Component(ComponentType.IMAGE, TARGET_IMAGE, new Json(targetImage), stackDtoService.getStackReferenceById(stackId));
            LOGGER.debug("Saving target image component for cluster upgrade. {}", component);
            componentConfigProviderService.store(component);
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

}
