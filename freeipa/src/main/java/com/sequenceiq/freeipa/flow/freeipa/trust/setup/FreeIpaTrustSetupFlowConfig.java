package com.sequenceiq.freeipa.flow.freeipa.trust.setup;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class FreeIpaTrustSetupFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent>
        implements RetryableFlowConfiguration<FreeIpaTrustSetupFlowEvent>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent>()
                    .defaultFailureEvent(FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FAILURE_EVENT)

                    .from(FreeIpaTrustSetupState.INIT_STATE)
                    .to(FreeIpaTrustSetupState.VALIDATION_STATE)
                    .event(FreeIpaTrustSetupFlowEvent.TRUST_SETUP_EVENT)
                    .defaultFailureEvent()

                    .from(FreeIpaTrustSetupState.VALIDATION_STATE)
                    .to(FreeIpaTrustSetupState.PREPARE_IPA_SERVER_STATE)
                    .event(FreeIpaTrustSetupFlowEvent.VALIDATION_FINISHED_EVENT)
                    .failureEvent(FreeIpaTrustSetupFlowEvent.VALIDATION_FAILED_EVENT)

                    .from(FreeIpaTrustSetupState.PREPARE_IPA_SERVER_STATE)
                    .to(FreeIpaTrustSetupState.CONFIGURE_DNS_STATE)
                    .event(FreeIpaTrustSetupFlowEvent.PREPARE_IPA_SERVER_FINISHED_EVENT)
                    .failureEvent(FreeIpaTrustSetupFlowEvent.PREPARE_IPA_SERVER_FAILED_EVENT)

                    .from(FreeIpaTrustSetupState.CONFIGURE_DNS_STATE)
                    .to(FreeIpaTrustSetupState.TRUST_SETUP_FINISHED_STATE)
                    .event(FreeIpaTrustSetupFlowEvent.CONFIGURE_DNS_FINISHED_EVENT)
                    .failureEvent(FreeIpaTrustSetupFlowEvent.CONFIGURE_DNS_FAILED_EVENT)

                    .from(FreeIpaTrustSetupState.TRUST_SETUP_FINISHED_STATE)
                    .to(FreeIpaTrustSetupState.FINAL_STATE)
                    .event(FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(FreeIpaTrustSetupState.INIT_STATE, FreeIpaTrustSetupState.FINAL_STATE,
                    FreeIpaTrustSetupState.TRUST_SETUP_FAILED_STATE, FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FAILURE_HANDLED_EVENT);

    public FreeIpaTrustSetupFlowConfig() {
        super(FreeIpaTrustSetupState.class, FreeIpaTrustSetupFlowEvent.class);
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (FreeIpaTrustSetupState.INIT_STATE.equals(flowState)) {
            return TRUST_SETUP_STARTED;
        } else if (FreeIpaTrustSetupState.TRUST_SETUP_FINISHED_STATE.equals(flowState)) {
            return TRUST_SETUP_FINISHED;
        } else if (FreeIpaTrustSetupState.TRUST_SETUP_FAILED_STATE.equals(flowState)) {
            return TRUST_SETUP_FAILED;
        } else {
            return UNSET;
        }
    }

    @Override
    protected List<Transition<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaTrustSetupFlowEvent[] getEvents() {
        return FreeIpaTrustSetupFlowEvent.values();
    }

    @Override
    public FreeIpaTrustSetupFlowEvent[] getInitEvents() {
        return new FreeIpaTrustSetupFlowEvent[]{FreeIpaTrustSetupFlowEvent.TRUST_SETUP_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Prepare Cross-realm Trust";
    }

    @Override
    public FreeIpaTrustSetupFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
