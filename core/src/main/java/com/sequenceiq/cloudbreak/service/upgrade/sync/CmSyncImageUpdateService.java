package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;

@Component
public class CmSyncImageUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncImageUpdateService.class);

    @Inject
    private StackImageService stackImageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CmSyncImageFinderService cmSyncImageFinderService;

    public void updateImageAfterCmSync(Stack stack, CmSyncOperationSummary cmSyncResult, Set<Image> candidateImages) {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = getCurrentImage(stack);
        Optional<Image> targetImageOpt = findTargetImage(cmSyncResult, candidateImages, currentImage.getImageId(), stack.isDatalake());
        if (targetImageOpt.isPresent()) {
            Image targetImage = targetImageOpt.get();
            if (!currentImage.getImageId().equals(targetImage.getUuid())) {
                LOGGER.debug("Set new image to {}", targetImage.getUuid());
                stackImageService.replaceStackImageComponent(stack, createStatedImage(targetImage, currentImage), currentImage);
                getStackVersionFromImage(targetImage).ifPresentOrElse(version -> updateStackVersion(stack, version),
                        () -> LOGGER.warn("Cluster runtime could not be upgraded for stack with id {}", stack.getId()));
            } else {
                LOGGER.debug("Not necessary to update the image because the current image already contains all the required parcels.");
            }
        } else {
            LOGGER.debug("There is no candidate image found for update the current image.");
        }
    }

    private void updateStackVersion(Stack stack, String version) {
        stackUpdater.updateStackVersion(stack.getId(), version);
        LOGGER.debug("Cluster runtime version on stack [{}] is updated from {} to {}", stack.getName(), stack.getStackVersion(), version);
    }

    private Optional<Image> findTargetImage(CmSyncOperationSummary cmSyncResult, Set<Image> candidateImages, String currentImageId, boolean datalake) {
        return cmSyncImageFinderService.findTargetImageForImageSync(cmSyncResult, candidateImages, currentImageId, datalake);
    }

    private Optional<String> getStackVersionFromImage(Image image) {
        return Optional.ofNullable(image.getStackDetails())
                .map(ImageStackDetails::getVersion);
    }

    private StatedImage createStatedImage(Image targetImage, com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return StatedImage.statedImage(targetImage, currentImage.getImageCatalogUrl(), currentImage.getImageCatalogName());
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image getCurrentImage(Stack stack) {
        try {
            return stackImageService.getCurrentImage(stack.getId());
        } catch (CloudbreakImageNotFoundException e) {
            String message = "Could not find image";
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
