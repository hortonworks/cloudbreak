package com.sequenceiq.freeipa.flow.stack.update;

import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_IN_DB_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_ON_PROVIDER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataState.UPDATE_USERDATA_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataState.UPDATE_USERDATA_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataState.UPDATE_USERDATA_ON_PROVIDER_STATE;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataState.UPDATE_USERDATA_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class UpdateUserDataFlowConfig extends StackStatusFinalizerAbstractFlowConfig<UpdateUserDataState, UpdateUserDataEvents> {

    private static final List<Transition<UpdateUserDataState, UpdateUserDataEvents>> TRANSITIONS =
            new Transition.Builder<UpdateUserDataState, UpdateUserDataEvents>()
                    .defaultFailureEvent(UpdateUserDataEvents.UPDATE_USERDATA_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(UPDATE_USERDATA_STATE)
                    .event(UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(UPDATE_USERDATA_STATE)
                    .to(UPDATE_USERDATA_ON_PROVIDER_STATE)
                    .event(UPDATE_USERDATA_IN_DB_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPDATE_USERDATA_ON_PROVIDER_STATE)
                    .to(UPDATE_USERDATA_FINISHED_STATE)
                    .event(UPDATE_USERDATA_ON_PROVIDER_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPDATE_USERDATA_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(UPDATE_USERDATA_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<UpdateUserDataState, UpdateUserDataEvents> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPDATE_USERDATA_FAILED_STATE, UPDATE_USERDATA_FAILURE_HANDLED_EVENT);

    public UpdateUserDataFlowConfig() {
        super(UpdateUserDataState.class, UpdateUserDataEvents.class);
    }

    @Override
    protected List<Transition<UpdateUserDataState, UpdateUserDataEvents>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<UpdateUserDataState, UpdateUserDataEvents> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpdateUserDataEvents[] getEvents() {
        return UpdateUserDataEvents.values();
    }

    @Override
    public UpdateUserDataEvents[] getInitEvents() {
        return new UpdateUserDataEvents[]{UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "UserData update";
    }
}
