package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.PREPARE_CROSS_REALM_TRUST_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.PREPARE_CROSS_REALM_TRUST_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.PREPARE_CROSS_REALM_TRUST_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.CONFIGURE_DNS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.CONFIGURE_DNS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.PREPARE_CROSS_REALM_TRUST_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.PREPARE_CROSS_REALM_TRUST_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.PREPARE_CROSS_REALM_TRUST_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.PREPARE_CROSS_REALM_TRUST_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.PREPARE_IPA_SERVER_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.PREPARE_IPA_SERVER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.VALIDATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustFlowEvent.VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustState.CONFIGURE_DNS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustState.PREPARE_CROSS_REALM_TRUST_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustState.PREPARE_CROSS_REALM_TRUST_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustState.PREPARE_IPA_SERVER_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustState.VALIDATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class FreeIpaPrepareCrossRealmTrustFlowConfig
        extends AbstractFlowConfiguration<FreeIpaPrepareCrossRealmTrustState, FreeIpaPrepareCrossRealmTrustFlowEvent>
        implements RetryableFlowConfiguration<FreeIpaPrepareCrossRealmTrustFlowEvent>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaPrepareCrossRealmTrustState, FreeIpaPrepareCrossRealmTrustFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaPrepareCrossRealmTrustState, FreeIpaPrepareCrossRealmTrustFlowEvent>()
                    .defaultFailureEvent(PREPARE_CROSS_REALM_TRUST_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(VALIDATION_STATE)
                    .event(PREPARE_CROSS_REALM_TRUST_EVENT)
                    .defaultFailureEvent()

                    .from(VALIDATION_STATE)
                    .to(PREPARE_IPA_SERVER_STATE)
                    .event(VALIDATION_FINISHED_EVENT)
                    .failureEvent(VALIDATION_FAILED_EVENT)

                    .from(PREPARE_IPA_SERVER_STATE)
                    .to(CONFIGURE_DNS_STATE)
                    .event(PREPARE_IPA_SERVER_FINISHED_EVENT)
                    .failureEvent(PREPARE_IPA_SERVER_FAILED_EVENT)

                    .from(CONFIGURE_DNS_STATE)
                    .to(PREPARE_CROSS_REALM_TRUST_FINISHED_STATE)
                    .event(CONFIGURE_DNS_FINISHED_EVENT)
                    .failureEvent(CONFIGURE_DNS_FAILED_EVENT)

                    .from(PREPARE_CROSS_REALM_TRUST_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(PREPARE_CROSS_REALM_TRUST_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaPrepareCrossRealmTrustState, FreeIpaPrepareCrossRealmTrustFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, PREPARE_CROSS_REALM_TRUST_FAILED_STATE, PREPARE_CROSS_REALM_TRUST_FAILURE_HANDLED_EVENT);

    public FreeIpaPrepareCrossRealmTrustFlowConfig() {
        super(FreeIpaPrepareCrossRealmTrustState.class, FreeIpaPrepareCrossRealmTrustFlowEvent.class);
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return PREPARE_CROSS_REALM_TRUST_STARTED;
        } else if (PREPARE_CROSS_REALM_TRUST_FINISHED_STATE.equals(flowState)) {
            return PREPARE_CROSS_REALM_TRUST_FINISHED;
        } else if (PREPARE_CROSS_REALM_TRUST_FAILED_STATE.equals(flowState)) {
            return PREPARE_CROSS_REALM_TRUST_FAILED;
        } else {
            return UNSET;
        }
    }

    @Override
    protected List<Transition<FreeIpaPrepareCrossRealmTrustState, FreeIpaPrepareCrossRealmTrustFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaPrepareCrossRealmTrustState, FreeIpaPrepareCrossRealmTrustFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaPrepareCrossRealmTrustFlowEvent[] getEvents() {
        return FreeIpaPrepareCrossRealmTrustFlowEvent.values();
    }

    @Override
    public FreeIpaPrepareCrossRealmTrustFlowEvent[] getInitEvents() {
        return new FreeIpaPrepareCrossRealmTrustFlowEvent[]{PREPARE_CROSS_REALM_TRUST_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Prepare Cross-realm Trust";
    }

    @Override
    public FreeIpaPrepareCrossRealmTrustFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
