package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;


import static com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_PARAMS_TRIGGER_EVENT;
import static com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsState.REFRESH_ENTITLEMENT_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsState.REFRESH_FREEIPA_ENTITLEMENT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class RefreshEntitlementParamsFlowConfig extends StackStatusFinalizerAbstractFlowConfig<RefreshEntitlementParamsState, RefreshEntitlementParamsEvent> {
    private static final List<Transition<RefreshEntitlementParamsState, RefreshEntitlementParamsEvent>> TRANSITIONS =
            new Builder<RefreshEntitlementParamsState, RefreshEntitlementParamsEvent>()
                    .defaultFailureEvent(REFRESH_ENTITLEMENT_FAILURE_EVENT)
                    .from(INIT_STATE).to(REFRESH_FREEIPA_ENTITLEMENT_STATE).event(REFRESH_ENTITLEMENT_PARAMS_TRIGGER_EVENT).defaultFailureEvent()
                    .from(REFRESH_FREEIPA_ENTITLEMENT_STATE).to(FINAL_STATE).event(REFRESH_ENTITLEMENT_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<RefreshEntitlementParamsState, RefreshEntitlementParamsEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            REFRESH_ENTITLEMENT_FAILED_STATE, REFRESH_ENTITLEMENT_FAIL_HANDLED_EVENT);

    public RefreshEntitlementParamsFlowConfig() {
        super(RefreshEntitlementParamsState.class, RefreshEntitlementParamsEvent.class);
    }

    @Override
    protected List<Transition<RefreshEntitlementParamsState, RefreshEntitlementParamsEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RefreshEntitlementParamsState, RefreshEntitlementParamsEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RefreshEntitlementParamsEvent[] getEvents() {
        return RefreshEntitlementParamsEvent.values();
    }

    @Override
    public RefreshEntitlementParamsEvent[] getInitEvents() {
        return new RefreshEntitlementParamsEvent[]{
                REFRESH_ENTITLEMENT_PARAMS_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Refresh dynamic entitlements";
    }

}
