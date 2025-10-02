package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.config;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CANCEL_TRUST_SETUP_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CANCEL_TRUST_SETUP_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CANCEL_TRUST_SETUP_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaTrustCancelState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaTrustCancelState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaTrustCancelState.TRUST_CANCEL_CONFIGURATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaTrustCancelState.TRUST_CANCEL_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaTrustCancelState.TRUST_CANCEL_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent.TRUST_CANCEL_CONFIGURATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent.TRUST_CANCEL_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent.TRUST_CANCEL_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent.TRUST_CANCEL_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent.TRUST_CANCEL_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent.TRUST_CANCEL_FINISHED_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaTrustCancelState;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent;

@Component
public class FreeIpaTrustCancelFlowConfig
    extends StackStatusFinalizerAbstractFlowConfig<FreeIpaTrustCancelState, FreeIpaTrustCancelFlowEvent>
    implements RetryableFlowConfiguration<FreeIpaTrustCancelFlowEvent>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaTrustCancelState, FreeIpaTrustCancelFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaTrustCancelState, FreeIpaTrustCancelFlowEvent>()
                    .defaultFailureEvent(TRUST_CANCEL_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(TRUST_CANCEL_CONFIGURATION_STATE)
                    .event(TRUST_CANCEL_EVENT)
                    .defaultFailureEvent()

                    .from(TRUST_CANCEL_CONFIGURATION_STATE)
                    .to(TRUST_CANCEL_FINISHED_STATE)
                    .event(TRUST_CANCEL_CONFIGURATION_FINISHED_EVENT)
                    .failureEvent(TRUST_CANCEL_CONFIGURATION_FAILED_EVENT)

                    .from(TRUST_CANCEL_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(TRUST_CANCEL_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaTrustCancelState, FreeIpaTrustCancelFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(
                    INIT_STATE,
                    FINAL_STATE,
                    TRUST_CANCEL_FAILED_STATE,
                    TRUST_CANCEL_FAILURE_HANDLED_EVENT
            );

    public FreeIpaTrustCancelFlowConfig() {
        super(FreeIpaTrustCancelState.class, FreeIpaTrustCancelFlowEvent.class);
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return CANCEL_TRUST_SETUP_STARTED;
        } else if (TRUST_CANCEL_FINISHED_STATE.equals(flowState)) {
            return CANCEL_TRUST_SETUP_FINISHED;
        } else if (TRUST_CANCEL_FAILED_STATE.equals(flowState)) {
            return CANCEL_TRUST_SETUP_FAILED;
        } else {
            return UNSET;
        }
    }

    @Override
    protected List<Transition<FreeIpaTrustCancelState, FreeIpaTrustCancelFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaTrustCancelState, FreeIpaTrustCancelFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaTrustCancelFlowEvent[] getEvents() {
        return FreeIpaTrustCancelFlowEvent.values();
    }

    @Override
    public FreeIpaTrustCancelFlowEvent[] getInitEvents() {
        return new FreeIpaTrustCancelFlowEvent[]{TRUST_CANCEL_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Cancel trust setup";
    }

    @Override
    public FreeIpaTrustCancelFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
