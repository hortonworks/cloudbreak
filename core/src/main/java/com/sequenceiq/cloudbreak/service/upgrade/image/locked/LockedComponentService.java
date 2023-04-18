package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;

@Service
public class LockedComponentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockedComponentService.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private LockedComponentChecker lockedComponentChecker;

    @Inject
    private ImageFilterParamsFactory imageFilterParamsFactory;

    public boolean isComponentsLocked(StackDto stack, String targetImageId) {
        try {
            Long workspaceId = stack.getWorkspaceId();
            Image currentImage = componentConfigProviderService.getImage(stack.getId());
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentCatalogImage = imageCatalogService
                    .getImage(workspaceId, currentImage.getImageCatalogUrl(), currentImage.getImageCatalogName(), currentImage.getImageId())
                    .getImage();
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = imageCatalogService
                    .getImage(workspaceId, currentImage.getImageCatalogUrl(), currentImage.getImageCatalogName(), targetImageId)
                    .getImage();
            return isComponentsLocked(stack, currentCatalogImage, targetCatalogImage);
        } catch (Exception ex) {
            String msg = "Exception during determining the lockComponents parameter.";
            LOGGER.warn(msg, ex);
            throw new CloudbreakRuntimeException(msg, ex);
        }
    }

    public boolean isComponentsLocked(StackDto stack, com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentCatalogImage,
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage) {
        LOGGER.debug("Determining that the stack {} component versions are the same on the current image {} and the target image {}", stack.getName(),
                currentCatalogImage.getUuid(), targetCatalogImage.getUuid());
        return lockedComponentChecker.isUpgradePermitted(currentCatalogImage, targetCatalogImage, imageFilterParamsFactory.getStackRelatedParcels(stack));
    }
}
