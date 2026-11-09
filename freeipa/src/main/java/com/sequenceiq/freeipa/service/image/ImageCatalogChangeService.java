package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_IMAGE_CATALOG_CHANGED;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
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

    @Inject
    private FreeipaPlatformStringTransformer platformStringTransformer;

    @Inject
    private EventSenderService eventSenderService;

    public void changeImageCatalog(String environmentCrn, String accountId, String imageCatalog) {
        try {
            final Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
            MDCBuilder.buildMdcContext(stack);
            if (flowLogService.isOtherFlowRunning(stack.getId())) {
                throw new CloudbreakServiceException(String.format("Operation is running for stack '%s'. Please try again later.", stack.getName()));
            }
            FreeIpaImageFilterSettings filterSettings = buildFilterSettingsForCatalogChange(imageCatalog, stack);
            String oldCatalog = displayedCatalog(stack.getImage());
            ImageEntity changedImage = imageService.changeImage(stack, filterSettings);
            sendEventIfCatalogChanged(stack, oldCatalog, displayedCatalog(changedImage));
        } catch (ImageNotFoundException e) {
            LOGGER.info("Could not find current image in new catalog", e);
            throw new CloudbreakServiceException("Could not find current image in new catalog", e);
        }
    }

    private void sendEventIfCatalogChanged(Stack stack, String oldCatalog, String newCatalog) {
        if (oldCatalog.equals(newCatalog)) {
            LOGGER.info("FreeIPA image catalog stays '{}', no catalog change event is sent", oldCatalog);
        } else {
            sendCatalogChangedEvent(stack, oldCatalog, newCatalog);
        }
    }

    private void sendCatalogChangedEvent(Stack stack, String oldCatalog, String newCatalog) {
        try {
            eventSenderService.sendEventAndNotification(stack, ThreadBasedUserCrnProvider.getUserCrn(), FREEIPA_IMAGE_CATALOG_CHANGED,
                    List.of(oldCatalog, newCatalog));
        } catch (RuntimeException e) {
            LOGGER.warn("FreeIPA image catalog changed from '{}' to '{}', but the catalog change event could not be sent", oldCatalog, newCatalog, e);
        }
    }

    private String displayedCatalog(ImageEntity image) {
        return StringUtils.defaultString(StringUtils.firstNonBlank(image.getImageCatalogUrl(), image.getImageCatalogName()));
    }

    private FreeIpaImageFilterSettings buildFilterSettingsForCatalogChange(String imageCatalog, Stack stack) {
        ImageEntity image = stack.getImage();
        String imageId = resolveCurrentImageIdForNewCatalog(stack, image);
        return new FreeIpaImageFilterSettings(
                imageId,
                imageCatalog,
                image.getOs(),
                image.getOs(),
                stack.getRegion(),
                platformStringTransformer.getPlatformString(stack),
                false,
                stack.getArchitecture()
        ).withMatchBySourceImageId(true);
    }

    private String resolveCurrentImageIdForNewCatalog(Stack stack, ImageEntity image) {
        try {
            String sourceImageId = imageService.getImageForStack(stack).getSourceImageId();
            if (StringUtils.isNotBlank(sourceImageId)) {
                LOGGER.info("Current image '{}' comes from a DB-based custom catalog; resolving the target image via source image id '{}'.",
                        image.getImageId(), sourceImageId);
                return sourceImageId;
            }
        } catch (ImageNotFoundException e) {
            LOGGER.warn("Could not look up the current image '{}' to determine its source image id; " +
                            "falling back to the stored image id '{}'. The target catalog lookup may fail if this is a custom catalog UUID. Error: {}",
                    image.getImageId(), image.getImageId(), e.getMessage());
        }
        return image.getImageId();
    }
}
