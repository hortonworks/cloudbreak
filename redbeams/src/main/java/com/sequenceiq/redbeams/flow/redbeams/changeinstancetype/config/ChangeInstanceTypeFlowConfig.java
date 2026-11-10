package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.config;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_MODIFY_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_SYNC_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_VALIDATION_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.CHANGE_INSTANCE_TYPE_WAIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_ALREADY_APPLIED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_START_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.FAILED_CHANGE_INSTANCE_TYPE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.FINALIZE_CHANGE_INSTANCE_TYPE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.ChangeInstanceTypeState;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors;

@Component
public class ChangeInstanceTypeFlowConfig extends AbstractFlowConfiguration<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors>
        implements RetryableFlowConfiguration<ChangeInstanceTypeStateSelectors> {

    private static final ChangeInstanceTypeStateSelectors[] INIT_EVENTS = {CHANGE_INSTANCE_TYPE_START_EVENT};

    private static final List<Transition<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors>> TRANSITIONS =
            new Transition.Builder<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors>()
                    .defaultFailureEvent(FAILED_CHANGE_INSTANCE_TYPE_EVENT)
                    .from(INIT_STATE).to(CHANGE_INSTANCE_TYPE_SYNC_STATE).event(CHANGE_INSTANCE_TYPE_START_EVENT).defaultFailureEvent()
                    .from(CHANGE_INSTANCE_TYPE_SYNC_STATE).to(CHANGE_INSTANCE_TYPE_VALIDATION_STATE).event(CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT)
                    .defaultFailureEvent()
                    .from(CHANGE_INSTANCE_TYPE_VALIDATION_STATE).to(CHANGE_INSTANCE_TYPE_MODIFY_STATE).event(CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT)
                    .defaultFailureEvent()
                    .from(CHANGE_INSTANCE_TYPE_VALIDATION_STATE).to(CHANGE_INSTANCE_TYPE_FINISHED_STATE).event(CHANGE_INSTANCE_TYPE_ALREADY_APPLIED_EVENT)
                    .defaultFailureEvent()
                    .from(CHANGE_INSTANCE_TYPE_MODIFY_STATE).to(CHANGE_INSTANCE_TYPE_WAIT_STATE).event(CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT)
                    .defaultFailureEvent()
                    .from(CHANGE_INSTANCE_TYPE_WAIT_STATE).to(CHANGE_INSTANCE_TYPE_FINISHED_STATE).event(CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT)
                    .defaultFailureEvent()
                    .from(CHANGE_INSTANCE_TYPE_FINISHED_STATE).to(FINAL_STATE).event(FINALIZE_CHANGE_INSTANCE_TYPE_EVENT).defaultFailureEvent()
                    .from(CHANGE_INSTANCE_TYPE_VALIDATION_STATE).to(CHANGE_INSTANCE_TYPE_FAILED_STATE).event(CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT)
                    .defaultFailureEvent()
                    .from(CHANGE_INSTANCE_TYPE_FAILED_STATE).to(FINAL_STATE).event(HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT).noFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CHANGE_INSTANCE_TYPE_FAILED_STATE, HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT);

    public ChangeInstanceTypeFlowConfig() {
        super(ChangeInstanceTypeState.class, ChangeInstanceTypeStateSelectors.class);
    }

    @Override
    protected List<Transition<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ChangeInstanceTypeStateSelectors[] getEvents() {
        return ChangeInstanceTypeStateSelectors.values();
    }

    @Override
    public ChangeInstanceTypeStateSelectors[] getInitEvents() {
        return INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Change database server instance type";
    }

    @Override
    public ChangeInstanceTypeStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT;
    }
}
