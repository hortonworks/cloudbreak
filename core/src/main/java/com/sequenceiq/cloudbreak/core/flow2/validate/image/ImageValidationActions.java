package com.sequenceiq.cloudbreak.core.flow2.validate.image;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.event.ImageValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationState;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateImageRequest;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class ImageValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageValidationActions.class);

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ImageService imageService;

    @Inject
    private StackImageService stackImageService;

    @Bean(name = "IMAGE_VALIDATION_STATE")
    public AbstractImageValidationAction<?> imageValidationAction() {
        return new AbstractImageValidationAction<>(ImageValidationTriggerEvent.class) {
            @Override
            protected void doExecute(StackContext context, ImageValidationTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                StackView stack = context.getStack().getStack();
                try {
                    Image currentImage = imageService.getImage(context.getStack().getId());
                    StatedImage statedImage = stackImageService.getStatedImageInternal(context.getStack().getStack())
                            .orElseThrow(() -> {
                                String message = String.format("Current image not found in the image catalog: %s", currentImage);
                                return new CloudbreakImageNotFoundException(message);
                            });
                    Image image = stackImageService.getImageModelFromStatedImage(stack, currentImage, statedImage);

                    ValidateImageRequest<Selectable> request =
                            new ValidateImageRequest<>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(), statedImage, image);
                    sendEvent(context, request);
                } catch (CloudbreakImageNotFoundException e) {
                    LOGGER.debug("Image not found", e);
                    throw new CloudbreakServiceException(e.getMessage(), e);
                }
            }
        };
    }

    @Bean(name = "IMAGE_VALIDATION_FAILED_STATE")
    public Action<?, ?> imageValidationFailureAction() {
        return new AbstractStackFailureAction<ImageValidationState, ImageValidationEvent>() {

            @Override
            protected StackFailureContext createFlowContext(FlowParameters flowParameters,
                    StateContext<ImageValidationState, ImageValidationEvent> stateContext, StackFailureEvent payload) {
                StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
                MDCBuilder.buildMdcContext(stack);
                return new StackFailureContext(flowParameters, stack, stack.getId());
            }

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                String statusReason = payload.getException().getMessage();
                stackUpdaterService.updateStatusAndSendEventWithArgs(context.getStackId(), DetailedStackStatus.REPAIR_FAILED,
                        ResourceEvent.IMAGE_VALIDATION_FAILED, statusReason, statusReason);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ImageValidationEvent.IMAGE_VALIDATION_FAILURE_HANDLED_EVENT.selector(), context.getStackId());
            }
        };
    }
}
