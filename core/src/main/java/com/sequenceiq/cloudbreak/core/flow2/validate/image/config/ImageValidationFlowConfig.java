package com.sequenceiq.cloudbreak.core.flow2.validate.image.config;

import static com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationEvent.IMAGE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationEvent.IMAGE_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationEvent.IMAGE_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationEvent.IMAGE_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationState.IMAGE_VALIDATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationState.IMAGE_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ImageValidationFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ImageValidationState, ImageValidationEvent>
        implements RetryableFlowConfiguration<ImageValidationEvent> {

    private static final List<Transition<ImageValidationState, ImageValidationEvent>> TRANSITIONS =
            new Builder<ImageValidationState, ImageValidationEvent>()
            .defaultFailureEvent(IMAGE_VALIDATION_FAILED_EVENT)
            .from(INIT_STATE).to(IMAGE_VALIDATION_STATE).event(IMAGE_VALIDATION_EVENT).defaultFailureEvent()
            .from(IMAGE_VALIDATION_STATE).to(FINAL_STATE).event(IMAGE_VALIDATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<ImageValidationState, ImageValidationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, IMAGE_VALIDATION_FAILED_STATE, IMAGE_VALIDATION_FAILURE_HANDLED_EVENT);

    public ImageValidationFlowConfig() {
        super(ImageValidationState.class, ImageValidationEvent.class);
    }

    @Override
    protected List<Transition<ImageValidationState, ImageValidationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ImageValidationState, ImageValidationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ImageValidationEvent[] getEvents() {
        return ImageValidationEvent.values();
    }

    @Override
    public ImageValidationEvent[] getInitEvents() {
        return new ImageValidationEvent[] {
                IMAGE_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Image validation for stack";
    }

    @Override
    public ImageValidationEvent getRetryableEvent() {
        return IMAGE_VALIDATION_FAILURE_HANDLED_EVENT;
    }
}
