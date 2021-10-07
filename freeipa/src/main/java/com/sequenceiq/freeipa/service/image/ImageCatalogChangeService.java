package com.sequenceiq.freeipa.service.image;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class ImageCatalogChangeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogChangeService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    @Inject
    private FlowLogService flowLogService;

    public void changeImageCatalog(String environmentCrn, String accountId, String imageCatalog) {
        try {
            final Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
            MDCBuilder.buildMdcContext(stack);
            if (flowLogService.isOtherFlowRunning(stack.getId())) {
                throw new CloudbreakServiceException(String.format("Operation is running for stack '%s'. Please try again later.", stack.getName()));
            }
            final ImageSettingsRequest imageRequest = getImageSettingsRequestForNewCatalogWithCurrentImageSettings(imageCatalog, stack.getImage());
            imageService.changeImage(stack, imageRequest);
        } catch (ImageNotFoundException e) {
            LOGGER.info("Could not find current image in new catalog", e);
            throw new CloudbreakServiceException("Could not find current image in new catalog", e);
        }
    }

    private ImageSettingsRequest getImageSettingsRequestForNewCatalogWithCurrentImageSettings(String imageCatalog, ImageEntity image) {
        final ImageSettingsRequest imageRequest = new ImageSettingsRequest();
        imageRequest.setCatalog(imageCatalog);
        imageRequest.setId(image.getImageId());
        imageRequest.setOs(image.getOs());
        return imageRequest;
    }
}
