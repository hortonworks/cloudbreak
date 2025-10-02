package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.config;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FINISH_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FINISH_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FINISH_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.FreeIpaTrustSetupFinishState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.FreeIpaTrustSetupFinishState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.FreeIpaTrustSetupFinishState.TRUST_SETUP_FINISH_ADD_TRUST_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.FreeIpaTrustSetupFinishState.TRUST_SETUP_FINISH_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.FreeIpaTrustSetupFinishState.TRUST_SETUP_FINISH_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent.TRUST_SETUP_FINISH_ADD_TRUST_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent.TRUST_SETUP_FINISH_ADD_TRUST_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent.TRUST_SETUP_FINISH_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent.TRUST_SETUP_FINISH_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent.TRUST_SETUP_FINISH_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent.TRUST_SETUP_FINISH_FINISHED_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.FreeIpaTrustSetupFinishState;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent;

@Component
public class FreeIpaTrustSetupFinishFlowConfig
    extends StackStatusFinalizerAbstractFlowConfig<FreeIpaTrustSetupFinishState, FreeIpaTrustSetupFinishFlowEvent>
    implements RetryableFlowConfiguration<FreeIpaTrustSetupFinishFlowEvent>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaTrustSetupFinishState, FreeIpaTrustSetupFinishFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaTrustSetupFinishState, FreeIpaTrustSetupFinishFlowEvent>()
                    .defaultFailureEvent(TRUST_SETUP_FINISH_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(TRUST_SETUP_FINISH_ADD_TRUST_STATE)
                    .event(TRUST_SETUP_FINISH_EVENT)
                    .defaultFailureEvent()

                    .from(TRUST_SETUP_FINISH_ADD_TRUST_STATE)
                    .to(TRUST_SETUP_FINISH_FINISHED_STATE)
                    .event(TRUST_SETUP_FINISH_ADD_TRUST_FINISHED_EVENT)
                    .failureEvent(TRUST_SETUP_FINISH_ADD_TRUST_FAILED_EVENT)

                    .from(TRUST_SETUP_FINISH_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(TRUST_SETUP_FINISH_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaTrustSetupFinishState, FreeIpaTrustSetupFinishFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(
                    INIT_STATE,
                    FINAL_STATE,
                    TRUST_SETUP_FINISH_FAILED_STATE,
                    TRUST_SETUP_FINISH_FAILURE_HANDLED_EVENT
            );

    public FreeIpaTrustSetupFinishFlowConfig() {
        super(FreeIpaTrustSetupFinishState.class, FreeIpaTrustSetupFinishFlowEvent.class);
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return TRUST_SETUP_FINISH_STARTED;
        } else if (TRUST_SETUP_FINISH_FINISHED_STATE.equals(flowState)) {
            return TRUST_SETUP_FINISH_FINISHED;
        } else if (TRUST_SETUP_FINISH_FAILED_STATE.equals(flowState)) {
            return TRUST_SETUP_FINISH_FAILED;
        } else {
            return UNSET;
        }
    }

    @Override
    protected List<Transition<FreeIpaTrustSetupFinishState, FreeIpaTrustSetupFinishFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaTrustSetupFinishState, FreeIpaTrustSetupFinishFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaTrustSetupFinishFlowEvent[] getEvents() {
        return FreeIpaTrustSetupFinishFlowEvent.values();
    }

    @Override
    public FreeIpaTrustSetupFinishFlowEvent[] getInitEvents() {
        return new FreeIpaTrustSetupFinishFlowEvent[]{TRUST_SETUP_FINISH_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Trust Setup Finish";
    }

    @Override
    public FreeIpaTrustSetupFinishFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
