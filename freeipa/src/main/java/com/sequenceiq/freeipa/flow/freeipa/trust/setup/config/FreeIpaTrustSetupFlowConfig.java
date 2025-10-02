package com.sequenceiq.freeipa.flow.freeipa.trust.setup.config;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.TRUST_SETUP_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState.TRUST_SETUP_CONFIGURE_DNS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState.TRUST_SETUP_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState.TRUST_SETUP_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState.TRUST_SETUP_PREPARE_IPA_SERVER_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState.TRUST_SETUP_VALIDATION_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_CONFIGURE_DNS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_CONFIGURE_DNS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_PREPARE_IPA_SERVER_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_PREPARE_IPA_SERVER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_VALIDATION_FINISHED_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent;

@Component
public class FreeIpaTrustSetupFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent>
        implements RetryableFlowConfiguration<FreeIpaTrustSetupFlowEvent>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent>()
                    .defaultFailureEvent(TRUST_SETUP_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(TRUST_SETUP_VALIDATION_STATE)
                    .event(TRUST_SETUP_EVENT)
                    .defaultFailureEvent()

                    .from(TRUST_SETUP_VALIDATION_STATE)
                    .to(TRUST_SETUP_PREPARE_IPA_SERVER_STATE)
                    .event(TRUST_SETUP_VALIDATION_FINISHED_EVENT)
                    .failureEvent(TRUST_SETUP_VALIDATION_FAILED_EVENT)

                    .from(TRUST_SETUP_PREPARE_IPA_SERVER_STATE)
                    .to(TRUST_SETUP_CONFIGURE_DNS_STATE)
                    .event(TRUST_SETUP_PREPARE_IPA_SERVER_FINISHED_EVENT)
                    .failureEvent(TRUST_SETUP_PREPARE_IPA_SERVER_FAILED_EVENT)

                    .from(TRUST_SETUP_CONFIGURE_DNS_STATE)
                    .to(TRUST_SETUP_FINISHED_STATE)
                    .event(TRUST_SETUP_CONFIGURE_DNS_FINISHED_EVENT)
                    .failureEvent(TRUST_SETUP_CONFIGURE_DNS_FAILED_EVENT)

                    .from(TRUST_SETUP_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(TRUST_SETUP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(
                    INIT_STATE,
                    FINAL_STATE,
                    TRUST_SETUP_FAILED_STATE,
                    TRUST_SETUP_FAILURE_HANDLED_EVENT
            );

    public FreeIpaTrustSetupFlowConfig() {
        super(FreeIpaTrustSetupState.class, FreeIpaTrustSetupFlowEvent.class);
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return TRUST_SETUP_STARTED;
        } else if (TRUST_SETUP_FINISHED_STATE.equals(flowState)) {
            return TRUST_SETUP_FINISHED;
        } else if (TRUST_SETUP_FAILED_STATE.equals(flowState)) {
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
        return new FreeIpaTrustSetupFlowEvent[]{TRUST_SETUP_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Setup Trust";
    }

    @Override
    public FreeIpaTrustSetupFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
