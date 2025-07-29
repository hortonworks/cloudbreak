package com.sequenceiq.freeipa.flow.freeipa.trust.cancel;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CANCEL_TRUST_SETUP_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CANCEL_TRUST_SETUP_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CANCEL_TRUST_SETUP_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupFlowEvent.CANCEL_TRUST_SETUP_CONFIGURATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupFlowEvent.CANCEL_TRUST_SETUP_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupFlowEvent.CANCEL_TRUST_SETUP_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupFlowEvent.CANCEL_TRUST_SETUP_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupFlowEvent.CANCEL_TRUST_SETUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupFlowEvent.CANCEL_TRUST_SETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class FreeIpaCancelTrustSetupFlowConfig
    extends AbstractFlowConfiguration<FreeIpaCancelTrustSetupState, FreeIpaCancelTrustSetupFlowEvent>
    implements RetryableFlowConfiguration<FreeIpaCancelTrustSetupFlowEvent>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaCancelTrustSetupState, FreeIpaCancelTrustSetupFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaCancelTrustSetupState, FreeIpaCancelTrustSetupFlowEvent>()
                    .defaultFailureEvent(CANCEL_TRUST_SETUP_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(FreeIpaCancelTrustSetupState.CANCEL_TRUST_SETUP_CONFIGURATION_STATE)
                    .event(CANCEL_TRUST_SETUP_EVENT)
                    .defaultFailureEvent()

                    .from(FreeIpaCancelTrustSetupState.CANCEL_TRUST_SETUP_CONFIGURATION_STATE)
                    .to(FreeIpaCancelTrustSetupState.CANCEL_TRUST_SETUP_FINISHED_STATE)
                    .event(CANCEL_TRUST_SETUP_CONFIGURATION_FINISHED_EVENT)
                    .failureEvent(CANCEL_TRUST_SETUP_CONFIGURATION_FAILED_EVENT)

                    .from(FreeIpaCancelTrustSetupState.CANCEL_TRUST_SETUP_FINISHED_STATE)
                    .to(FreeIpaCancelTrustSetupState.FINAL_STATE)
                    .event(CANCEL_TRUST_SETUP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaCancelTrustSetupState, FreeIpaCancelTrustSetupFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FreeIpaCancelTrustSetupState.FINAL_STATE,
                    FreeIpaCancelTrustSetupState.CANCEL_TRUST_SETUP_FAILED_STATE, CANCEL_TRUST_SETUP_FAILURE_HANDLED_EVENT);

    public FreeIpaCancelTrustSetupFlowConfig() {
        super(FreeIpaCancelTrustSetupState.class, FreeIpaCancelTrustSetupFlowEvent.class);
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return CANCEL_TRUST_SETUP_STARTED;
        } else if (FreeIpaCancelTrustSetupState.CANCEL_TRUST_SETUP_FINISHED_STATE.equals(flowState)) {
            return CANCEL_TRUST_SETUP_FINISHED;
        } else if (FreeIpaCancelTrustSetupState.CANCEL_TRUST_SETUP_FAILED_STATE.equals(flowState)) {
            return CANCEL_TRUST_SETUP_FAILED;
        } else {
            return UNSET;
        }
    }

    @Override
    protected List<Transition<FreeIpaCancelTrustSetupState, FreeIpaCancelTrustSetupFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaCancelTrustSetupState, FreeIpaCancelTrustSetupFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaCancelTrustSetupFlowEvent[] getEvents() {
        return FreeIpaCancelTrustSetupFlowEvent.values();
    }

    @Override
    public FreeIpaCancelTrustSetupFlowEvent[] getInitEvents() {
        return new FreeIpaCancelTrustSetupFlowEvent[]{CANCEL_TRUST_SETUP_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Cancel trust setup";
    }

    @Override
    public FreeIpaCancelTrustSetupFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
