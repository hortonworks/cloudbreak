package com.sequenceiq.cloudbreak.service.upgrade;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class UpgradeImageInfoFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeImageInfoFactory.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackService stackService;

    public UpgradeImageInfo create(String targetImageId, Long stackId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = componentConfigProviderService.getImage(stackId);
        Stack stack = stackService.get(stackId);
        StatedImage targetStatedImage = imageCatalogService
                .getImage(stack.getWorkspace().getId(), currentImage.getImageCatalogUrl(), currentImage.getImageCatalogName(), targetImageId);
        UpgradeImageInfo upgradeImageInfo = new UpgradeImageInfo(currentImage, targetStatedImage);
        LOGGER.debug("Provided upgradeImageInfo values to create UpgradeImageInfo: {}", upgradeImageInfo);
        return upgradeImageInfo;
    }

}
