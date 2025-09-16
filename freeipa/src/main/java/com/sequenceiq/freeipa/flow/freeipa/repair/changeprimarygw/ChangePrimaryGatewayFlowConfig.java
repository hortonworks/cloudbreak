package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw;

import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_METADATA_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_SELECTION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_STARTING_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_FAIL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_METADATA_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_SELECTION;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_STATE_STARTING;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.CHANGE_PRIMARY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class ChangePrimaryGatewayFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> {
    private static final List<Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent>> TRANSITIONS =
            new Transition.Builder<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent>()
                    .defaultFailureEvent(FAILURE_EVENT)

                    .from(INIT_STATE).to(CHANGE_PRIMARY_GATEWAY_STATE_STARTING)
                    .event(CHANGE_PRIMARY_GATEWAY_EVENT)
                    .defaultFailureEvent()

                    .from(CHANGE_PRIMARY_GATEWAY_STATE_STARTING).to(CHANGE_PRIMARY_GATEWAY_SELECTION)
                    .event(CHANGE_PRIMARY_GATEWAY_STARTING_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CHANGE_PRIMARY_GATEWAY_SELECTION).to(CHANGE_PRIMARY_GATEWAY_METADATA_STATE)
                    .event(CHANGE_PRIMARY_GATEWAY_SELECTION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CHANGE_PRIMARY_GATEWAY_METADATA_STATE).to(CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_STATE)
                    .event(CHANGE_PRIMARY_GATEWAY_METADATA_FINISHED_EVENT)
                    .failureEvent(CHANGE_PRIMARY_GATEWAY_METADATA_FAILED_EVENT)

                    .from(CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_STATE).to(CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_STATE)
                    .event(CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_FINISHED_EVENT)
                    .failureEvent(CHANGE_PRIMARY_GATEWAY_CLUSTERPROXY_REGISTRATION_FAILED_EVENT)

                    .from(CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_STATE).to(CHANGE_PRIMARY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_STATE)
                    .event(CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_FINISHED_EVENT)
                    .failureEvent(CHANGE_PRIMARY_GATEWAY_HEALTH_CHECK_FAILED_EVENT)

                    .from(CHANGE_PRIMARY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_STATE).to(CHANGE_PRIMARY_GATEWAY_FINISHED_STATE)
                    .event(CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CHANGE_PRIMARY_GATEWAY_FINISHED_STATE).to(FINAL_STATE)
                    .event(CHANGE_PRIMARY_GATEWAY_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CHANGE_PRIMARY_GATEWAY_FAIL_STATE, FAIL_HANDLED_EVENT);

    public ChangePrimaryGatewayFlowConfig() {
        super(ChangePrimaryGatewayState.class, ChangePrimaryGatewayFlowEvent.class);
    }

    @Override
    protected List<Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ChangePrimaryGatewayState, ChangePrimaryGatewayFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ChangePrimaryGatewayFlowEvent[] getEvents() {
        return ChangePrimaryGatewayFlowEvent.values();
    }

    @Override
    public ChangePrimaryGatewayFlowEvent[] getInitEvents() {
        return new ChangePrimaryGatewayFlowEvent[] { CHANGE_PRIMARY_GATEWAY_EVENT };
    }

    @Override
    public String getDisplayName() {
        return "Change FreeIPA Primary Gateway";
    }
}
