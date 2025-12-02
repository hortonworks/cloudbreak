package com.sequenceiq.freeipa.flow.stack.image.change.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGED_IN_DB_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_NOT_REQUIRED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;
import com.sequenceiq.freeipa.service.image.ImageService;

public class ImageChangeAction extends AbstractImageChangeAction<ImageChangeEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageChangeActions.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ImageRevisionReaderService imageRevisionReaderService;

    public ImageChangeAction() {
        super(ImageChangeEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, ImageChangeEvent payload, Map<Object, Object> variables) throws Exception {
        setOperationId(variables, payload.getOperationId());
        ImageEntity originalImageEntity = storeOriginalImageRevision(payload, variables);
        ImageEntity newImageEntity = imageService.changeImage(context.getStack(), payload.getRequest());
        variables.put(ImageChangeActions.IMAGE_CHANGED_IN_DB, Boolean.TRUE);
        if (originalImageEntity.equals(newImageEntity)) {
            LOGGER.info("New and original image is the same, no change is required");
            sendEvent(context, new ImageChangeEvent(IMAGE_CHANGE_NOT_REQUIRED_EVENT.event(), payload.getResourceId(), payload.getRequest()));
        } else {
            sendEvent(context, new ImageChangeEvent(IMAGE_CHANGED_IN_DB_EVENT.event(), payload.getResourceId(), payload.getRequest()));
        }
    }

    private ImageEntity storeOriginalImageRevision(ImageChangeEvent payload, Map<Object, Object> variables) {
        ImageEntity imageEntity = imageService.getByStackId(payload.getResourceId());
        Long imageEntityId = imageEntity.getId();
        List<Number> revisions = imageRevisionReaderService.getRevisions(imageEntityId);
        LOGGER.debug("Revisions found for current image with id {} are: {}", imageEntityId, revisions);
        if (!revisions.isEmpty()) {
            storeRevisionInfo(variables, imageEntityId, revisions);
        } else {
            LOGGER.info("No revision found for current image with id [{}]", imageEntityId);
            variables.put(ImageChangeActions.ORIGINAL_IMAGE, imageEntity);
        }
        return imageEntity;
    }

    private void storeRevisionInfo(Map<Object, Object> variables, Long imageEntityId, List<Number> revisions) {
        if (!revisions.isEmpty()) {
            Number latestRevision = revisions.get(revisions.size() - 1);
            LOGGER.info("Original image revision is [{}] for image with id [{}]", latestRevision, imageEntityId);
            variables.put(ImageChangeActions.ORIGINAL_IMAGE_REVISION, latestRevision);
            variables.put(ImageChangeActions.IMAGE_ENTITY_ID, imageEntityId);
        }
    }

    @Override
    protected Object getFailurePayload(ImageChangeEvent payload, Optional<StackContext> flowContext, Exception ex) {
        LOGGER.error("[CHANGE_IMAGE_STATE] failed", ex);
        return new StackFailureEvent(IMAGE_CHANGE_FAILED_EVENT.event(), payload.getResourceId(), ex, ERROR);
    }
}
