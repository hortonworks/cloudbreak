package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmEvent.UPDATE_TRUSTED_REALM_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmEvent.UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmEvent.UPDATE_TRUSTED_REALM_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmEvent.UPDATE_TRUSTED_REALM_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmEvent.UPDATE_TRUSTED_REALM_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmState.UPDATE_TRUSTED_REALM_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmState.UPDATE_TRUSTED_REALM_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmState.UPDATE_TRUSTED_REALM_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;

@Component
public class UpdateTrustedRealmFlowConfig extends StackStatusFinalizerAbstractFlowConfig<UpdateTrustedRealmState, UpdateTrustedRealmEvent> {

    private static final List<Transition<UpdateTrustedRealmState, UpdateTrustedRealmEvent>> TRANSITIONS =
            new Transition.Builder<UpdateTrustedRealmState, UpdateTrustedRealmEvent>()
                    .defaultFailureEvent(UPDATE_TRUSTED_REALM_FAILED_EVENT)

                    .from(INIT_STATE).to(UPDATE_TRUSTED_REALM_STATE).event(UPDATE_TRUSTED_REALM_TRIGGER_EVENT).noFailureEvent()
                    .from(UPDATE_TRUSTED_REALM_STATE).to(UPDATE_TRUSTED_REALM_FINISHED_STATE).event(UPDATE_TRUSTED_REALM_SUCCESS_EVENT)
                        .defaultFailureEvent()
                    .from(UPDATE_TRUSTED_REALM_FINISHED_STATE).to(FINAL_STATE).event(UPDATE_TRUSTED_REALM_FINALIZED_EVENT)
                        .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<UpdateTrustedRealmState, UpdateTrustedRealmEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPDATE_TRUSTED_REALM_FAILED_STATE, UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT);

    public UpdateTrustedRealmFlowConfig() {
        super(UpdateTrustedRealmState.class, UpdateTrustedRealmEvent.class);
    }

    @Override
    protected List<Transition<UpdateTrustedRealmState, UpdateTrustedRealmEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<UpdateTrustedRealmState, UpdateTrustedRealmEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpdateTrustedRealmEvent[] getEvents() {
        return UpdateTrustedRealmEvent.values();
    }

    @Override
    public UpdateTrustedRealmEvent[] getInitEvents() {
        return new UpdateTrustedRealmEvent[]{ UPDATE_TRUSTED_REALM_TRIGGER_EVENT };
    }

    @Override
    public String getDisplayName() {
        return "Update trusted realm";
    }
}

