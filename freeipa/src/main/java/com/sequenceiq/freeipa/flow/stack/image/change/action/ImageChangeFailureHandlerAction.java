package com.sequenceiq.freeipa.flow.stack.image.change.action;

import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_FAILURE_HANDLED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.operation.OperationService;

class ImageChangeFailureHandlerAction extends AbstractStackFailureAction<ImageChangeState, ImageChangeEvents> implements OperationAwareAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageChangeFailureHandlerAction.class);

    @Inject
    private ImageService imageService;

    @Inject
    private OperationService operationService;

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.error("Image change failed", payload.getException());
        if (variables.containsKey(ImageChangeActions.ORIGINAL_IMAGE_REVISION) && variables.containsKey(ImageChangeActions.IMAGE_CHANGED_IN_DB)) {
            LOGGER.info("Reverting to original image using revision [{}]", variables.get(ImageChangeActions.ORIGINAL_IMAGE_REVISION));
            imageService.revertImageToRevision(
                    context.getStack().getAccountId(),
                    (Long) variables.get(ImageChangeActions.IMAGE_ENTITY_ID),
                    (Number) variables.get(ImageChangeActions.ORIGINAL_IMAGE_REVISION));
        } else if (variables.containsKey(ImageChangeActions.ORIGINAL_IMAGE) && variables.containsKey(ImageChangeActions.IMAGE_CHANGED_IN_DB)) {
            LOGGER.info("Reverting to original image using entity stored in variables");
            ImageEntity originalImage = (ImageEntity) variables.get(ImageChangeActions.ORIGINAL_IMAGE);
            imageService.save(originalImage);
        }
        if (isOperationIdSet(variables)) {
            operationService.failOperation(context.getStack().getAccountId(), getOperationId(variables), payload.getException().getMessage());
        }
        getStackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.IMAGE_CHANGE_FAILED,
                "Image change failed with: " + payload.getException().getMessage());
        getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(), ResourceEvent.FREEIPA_IMAGE_CHANGE_FAILED,
                List.of(Optional.ofNullable(payload.getException()).map(Throwable::getMessage).orElse("Unknown")));
        sendEvent(context, new StackEvent(IMAGE_CHANGE_FAILURE_HANDLED_EVENT.event(), context.getStack().getId()));
    }
}
