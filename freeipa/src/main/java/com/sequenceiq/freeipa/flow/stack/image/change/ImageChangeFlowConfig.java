package com.sequenceiq.freeipa.flow.stack.image.change;

import static com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState.CHANGE_IMAGE_STATE;
import static com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState.CHECK_IMAGE_STATE;
import static com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState.IMAGE_CHANGE_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState.IMAGE_CHANGE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState.PREPARE_IMAGE_STATE;
import static com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState.SET_FALLBACK_IMAGE_STATE;
import static com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState.SET_IMAGE_ON_PROVIDER_STATE;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGED_IN_DB_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_NOT_REQUIRED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_FALLBACK_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_FALLBACK_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_FALLBACK_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_PREPARATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.SET_IMAGE_ON_PROVIDER_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.SET_IMAGE_ON_PROVIDER_FINISHED_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents;

@Component
public class ImageChangeFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ImageChangeState, ImageChangeEvents> {

    private static final List<Transition<ImageChangeState, ImageChangeEvents>> TRANSITIONS = new Transition.Builder<ImageChangeState, ImageChangeEvents>()
            .defaultFailureEvent(ImageChangeEvents.IMAGE_CHANGE_FAILED_EVENT)

            .from(INIT_STATE)
            .to(CHANGE_IMAGE_STATE)
            .event(ImageChangeEvents.IMAGE_CHANGE_EVENT)
            .noFailureEvent()

            .from(CHANGE_IMAGE_STATE)
            .to(PREPARE_IMAGE_STATE)
            .event(IMAGE_CHANGED_IN_DB_EVENT)
            .defaultFailureEvent()

            .from(CHANGE_IMAGE_STATE)
            .to(IMAGE_CHANGE_FINISHED_STATE)
            .event(IMAGE_CHANGE_NOT_REQUIRED_EVENT)
            .defaultFailureEvent()

            .from(PREPARE_IMAGE_STATE)
            .to(CHECK_IMAGE_STATE)
            .event(IMAGE_PREPARATION_FINISHED_EVENT)
            .failureEvent(IMAGE_PREPARATION_FAILED_EVENT)

            .from(PREPARE_IMAGE_STATE)
            .to(SET_FALLBACK_IMAGE_STATE)
            .event(IMAGE_FALLBACK_EVENT)
            .failureEvent(IMAGE_PREPARATION_FAILED_EVENT)

            .from(SET_FALLBACK_IMAGE_STATE)
            .to(CHECK_IMAGE_STATE)
            .event(IMAGE_FALLBACK_FINISHED_EVENT)
            .failureEvent(IMAGE_FALLBACK_FAILED_EVENT)

            .from(CHECK_IMAGE_STATE)
            .to(CHECK_IMAGE_STATE)
            .event(IMAGE_COPY_CHECK_EVENT)
            .defaultFailureEvent()

            .from(CHECK_IMAGE_STATE)
            .to(SET_IMAGE_ON_PROVIDER_STATE)
            .event(IMAGE_COPY_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(SET_IMAGE_ON_PROVIDER_STATE)
            .to(IMAGE_CHANGE_FINISHED_STATE)
            .event(SET_IMAGE_ON_PROVIDER_FINISHED_EVENT)
            .failureEvent(SET_IMAGE_ON_PROVIDER_FAILED_EVENT)

            .from(IMAGE_CHANGE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(IMAGE_CHANGE_FINISHED_EVENT)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<ImageChangeState, ImageChangeEvents> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, IMAGE_CHANGE_FAILED_STATE, ImageChangeEvents.IMAGE_CHANGE_FAILURE_HANDLED_EVENT);

    public ImageChangeFlowConfig() {
        super(ImageChangeState.class, ImageChangeEvents.class);
    }

    @Override
    protected List<Transition<ImageChangeState, ImageChangeEvents>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ImageChangeState, ImageChangeEvents> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ImageChangeEvents[] getEvents() {
        return ImageChangeEvents.values();
    }

    @Override
    public ImageChangeEvents[] getInitEvents() {
        return new ImageChangeEvents[]{ImageChangeEvents.IMAGE_CHANGE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Image change";
    }
}
