package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.CHECK_IMAGE_VERESIONS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.CHECK_PACKAGE_VERSIONS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.IMAGE_PREPARATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.IMAGE_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.SET_IMAGE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.SET_IMAGE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.STACK_IMAGE_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.CHECK_IMAGE_VERSIONS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.CHECK_PACKAGE_VERSIONS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.IMAGE_CHECK_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.IMAGE_PREPARE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.SET_IMAGE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.STACK_IMAGE_UPDATE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.STACK_IMAGE_UPDATE_FINISHED;
import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateState.UPDATE_IMAGE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class StackImageUpdateFlowConfig extends AbstractFlowConfiguration<StackImageUpdateState, StackImageUpdateEvent> {

    private static final List<Transition<StackImageUpdateState, StackImageUpdateEvent>> TRANSITIONS =
            new Builder<StackImageUpdateState, StackImageUpdateEvent>().defaultFailureEvent(STACK_IMAGE_UPDATE_FAILED_EVENT)
                    .from(INIT_STATE).to(CHECK_IMAGE_VERSIONS_STATE).event(STACK_IMAGE_UPDATE_EVENT).noFailureEvent()
                    .from(CHECK_IMAGE_VERSIONS_STATE).to(CHECK_PACKAGE_VERSIONS_STATE).event(CHECK_IMAGE_VERESIONS_FINISHED_EVENT).defaultFailureEvent()
                    .from(CHECK_PACKAGE_VERSIONS_STATE).to(UPDATE_IMAGE_STATE).event(CHECK_PACKAGE_VERSIONS_FINISHED_EVENT).defaultFailureEvent()
                    .from(UPDATE_IMAGE_STATE).to(IMAGE_PREPARE_STATE).event(UPDATE_IMAGE_FINESHED_EVENT).defaultFailureEvent()
                    .from(IMAGE_PREPARE_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_PREPARATION_FINISHED_EVENT).failureEvent(IMAGE_PREPARATION_FAILED_EVENT)
                    .from(IMAGE_CHECK_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_COPY_CHECK_EVENT).defaultFailureEvent()
                    .from(IMAGE_CHECK_STATE).to(SET_IMAGE_STATE).event(IMAGE_COPY_FINISHED_EVENT).defaultFailureEvent()
                    .from(SET_IMAGE_STATE).to(STACK_IMAGE_UPDATE_FINISHED).event(SET_IMAGE_FINISHED_EVENT).failureEvent(SET_IMAGE_FAILED_EVENT)
                    .from(STACK_IMAGE_UPDATE_FINISHED).to(FINAL_STATE).event(STACK_IMAGE_UPDATE_FINISHED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<StackImageUpdateState, StackImageUpdateEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STACK_IMAGE_UPDATE_FAILED_STATE, STACK_IMAGE_UPDATE_FAILE_HANDLED_EVENT);

    public StackImageUpdateFlowConfig() {
        super(StackImageUpdateState.class, StackImageUpdateEvent.class);
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(StackImageUpdateFlowTriggerCondition.class);
    }

    @Override
    protected List<Transition<StackImageUpdateState, StackImageUpdateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackImageUpdateState, StackImageUpdateEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackImageUpdateEvent[] getEvents() {
        return StackImageUpdateEvent.values();
    }

    @Override
    public StackImageUpdateEvent[] getInitEvents() {
        return new StackImageUpdateEvent[]{STACK_IMAGE_UPDATE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Update stack image";
    }
}
