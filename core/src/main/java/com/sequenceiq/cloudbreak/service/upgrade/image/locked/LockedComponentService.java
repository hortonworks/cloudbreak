package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogProvider;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;

@Service
public class LockedComponentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockedComponentService.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageProvider imageProvider;

    @Inject
    private LockedComponentChecker lockedComponentChecker;

    @Inject
    private ImageFilterParamsFactory imageFilterParamsFactory;

    public boolean isComponentsLocked(Stack stack, String targetImageId) {
        try {
            Image currentImage = componentConfigProviderService.getImage(stack.getId());
            CloudbreakImageCatalogV3 imageCatalog = imageCatalogProvider.getImageCatalogV3(currentImage.getImageCatalogUrl());
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentCatalogImage = imageProvider.getCurrentImageFromCatalog(currentImage.getImageId(),
                    imageCatalog);
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = imageProvider.getCurrentImageFromCatalog(targetImageId, imageCatalog);
            LOGGER.info("Determining that the stack {} component versions are the same on the current image {} and the target image {}", stack.getName(),
                    currentCatalogImage.getUuid(), targetCatalogImage.getUuid());
            return lockedComponentChecker.isUpgradePermitted(currentCatalogImage, targetCatalogImage, imageFilterParamsFactory.getStackRelatedParcels(stack));
        } catch (Exception ex) {
            String msg = "Exception during determining the lockComponents parameter.";
            LOGGER.warn(msg, ex);
            throw new CloudbreakRuntimeException(msg, ex);
        }
    }
}
