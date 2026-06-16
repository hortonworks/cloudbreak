package com.sequenceiq.datalake.flow.trustedrealm;

import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_EVENT;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.INIT_STATE;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.UPDATE_TRUSTED_REALM_FAILED_STATE;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.UPDATE_TRUSTED_REALM_SUCCESS_STATE;
import static com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerState.UPDATE_TRUSTED_REALM_WAITING_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class UpdateTrustedRealmTrackerFlowConfig extends AbstractFlowConfiguration<UpdateTrustedRealmTrackerState, UpdateTrustedRealmTrackerEvent> {

    private static final List<Transition<UpdateTrustedRealmTrackerState, UpdateTrustedRealmTrackerEvent>> TRANSITIONS =
            new Transition.Builder<UpdateTrustedRealmTrackerState, UpdateTrustedRealmTrackerEvent>()
                    .defaultFailureEvent(UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_FAILED_EVENT)

                    .from(INIT_STATE).to(UPDATE_TRUSTED_REALM_WAITING_STATE).event(UPDATE_TRUSTED_REALM_EVENT).noFailureEvent()
                    .from(UPDATE_TRUSTED_REALM_WAITING_STATE).to(UPDATE_TRUSTED_REALM_SUCCESS_STATE).event(UPDATE_TRUSTED_REALM_SUCCESS_EVENT)
                    .defaultFailureEvent()
                    .from(UPDATE_TRUSTED_REALM_SUCCESS_STATE).to(FINAL_STATE).event(UPDATE_TRUSTED_REALM_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<UpdateTrustedRealmTrackerState, UpdateTrustedRealmTrackerEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPDATE_TRUSTED_REALM_FAILED_STATE, UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT);

    public UpdateTrustedRealmTrackerFlowConfig() {
        super(UpdateTrustedRealmTrackerState.class, UpdateTrustedRealmTrackerEvent.class);
    }

    @Override
    protected List<Transition<UpdateTrustedRealmTrackerState, UpdateTrustedRealmTrackerEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<UpdateTrustedRealmTrackerState, UpdateTrustedRealmTrackerEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpdateTrustedRealmTrackerEvent[] getEvents() {
        return UpdateTrustedRealmTrackerEvent.values();
    }

    @Override
    public UpdateTrustedRealmTrackerEvent[] getInitEvents() {
        return new UpdateTrustedRealmTrackerEvent[]{
                UPDATE_TRUSTED_REALM_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Update trusted realm";
    }
}
