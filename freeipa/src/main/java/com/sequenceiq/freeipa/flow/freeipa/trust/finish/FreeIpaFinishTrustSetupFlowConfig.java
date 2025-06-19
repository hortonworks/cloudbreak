package com.sequenceiq.freeipa.flow.freeipa.trust.finish;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FINISH_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FINISH_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FINISH_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent.ADD_TRUST_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent.ADD_TRUST_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent.FINISH_TRUST_SETUP_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent.FINISH_TRUST_SETUP_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent.FINISH_TRUST_SETUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent.FINISH_TRUST_SETUP_FINISHED_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState;

@Component
public class FreeIpaFinishTrustSetupFlowConfig
    extends AbstractFlowConfiguration<FreeIpaFinishTrustSetupState, FreeIpaFinishTrustSetupFlowEvent>
    implements RetryableFlowConfiguration<FreeIpaFinishTrustSetupFlowEvent>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaFinishTrustSetupState, FreeIpaFinishTrustSetupFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaFinishTrustSetupState, FreeIpaFinishTrustSetupFlowEvent>()
                    .defaultFailureEvent(FINISH_TRUST_SETUP_FAILURE_EVENT)

                    .from(FreeIpaFinishTrustSetupState.INIT_STATE)
                    .to(FreeIpaFinishTrustSetupState.ADD_TRUST_STATE)
                    .event(FINISH_TRUST_SETUP_EVENT)
                    .defaultFailureEvent()

                    .from(FreeIpaFinishTrustSetupState.ADD_TRUST_STATE)
                    .to(FreeIpaFinishTrustSetupState.FINISH_TRUST_SETUP_FINISHED_STATE)
                    .event(ADD_TRUST_FINISHED_EVENT)
                    .failureEvent(ADD_TRUST_FAILED_EVENT)

                    .from(FreeIpaFinishTrustSetupState.FINISH_TRUST_SETUP_FINISHED_STATE)
                    .to(FreeIpaFinishTrustSetupState.FINAL_STATE)
                    .event(FINISH_TRUST_SETUP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaFinishTrustSetupState, FreeIpaFinishTrustSetupFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(FreeIpaFinishTrustSetupState.INIT_STATE, FreeIpaFinishTrustSetupState.FINAL_STATE,
                    FreeIpaFinishTrustSetupState.FINISH_TRUST_SETUP_FAILED_STATE, FINISH_TRUST_SETUP_FAILURE_HANDLED_EVENT);

    public FreeIpaFinishTrustSetupFlowConfig() {
        super(FreeIpaFinishTrustSetupState.class, FreeIpaFinishTrustSetupFlowEvent.class);
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (FreeIpaTrustSetupState.INIT_STATE.equals(flowState)) {
            return TRUST_SETUP_FINISH_STARTED;
        } else if (FreeIpaFinishTrustSetupState.FINISH_TRUST_SETUP_FINISHED_STATE.equals(flowState)) {
            return TRUST_SETUP_FINISH_FINISHED;
        } else if (FreeIpaFinishTrustSetupState.FINISH_TRUST_SETUP_FAILED_STATE.equals(flowState)) {
            return TRUST_SETUP_FINISH_FAILED;
        } else {
            return UNSET;
        }
    }

    @Override
    protected List<Transition<FreeIpaFinishTrustSetupState, FreeIpaFinishTrustSetupFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaFinishTrustSetupState, FreeIpaFinishTrustSetupFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaFinishTrustSetupFlowEvent[] getEvents() {
        return FreeIpaFinishTrustSetupFlowEvent.values();
    }

    @Override
    public FreeIpaFinishTrustSetupFlowEvent[] getInitEvents() {
        return new FreeIpaFinishTrustSetupFlowEvent[]{FINISH_TRUST_SETUP_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Finish trust setup";
    }

    @Override
    public FreeIpaFinishTrustSetupFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
