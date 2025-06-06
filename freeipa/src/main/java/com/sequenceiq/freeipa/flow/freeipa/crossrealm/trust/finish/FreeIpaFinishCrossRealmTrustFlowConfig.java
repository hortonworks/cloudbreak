package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.FINISH_CROSS_REALM_TRUST_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.FINISH_CROSS_REALM_TRUST_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.FINISH_CROSS_REALM_TRUST_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustFlowEvent.ADD_TRUST_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustFlowEvent.ADD_TRUST_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustFlowEvent.FINISH_CROSS_REALM_TRUST_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustFlowEvent.FINISH_CROSS_REALM_TRUST_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustFlowEvent.FINISH_CROSS_REALM_TRUST_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustFlowEvent.FINISH_CROSS_REALM_TRUST_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustState.ADD_TRUST_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustState.FINISH_CROSS_REALM_TRUST_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustState.FINISH_CROSS_REALM_TRUST_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.FreeIpaPrepareCrossRealmTrustState;

@Component
public class FreeIpaFinishCrossRealmTrustFlowConfig
    extends AbstractFlowConfiguration<FreeIpaFinishCrossRealmTrustState, FreeIpaFinishCrossRealmTrustFlowEvent>
    implements RetryableFlowConfiguration<FreeIpaFinishCrossRealmTrustFlowEvent>, FreeIpaUseCaseAware {

    private static final List<Transition<FreeIpaFinishCrossRealmTrustState, FreeIpaFinishCrossRealmTrustFlowEvent>> TRANSITIONS =
            new Transition.Builder<FreeIpaFinishCrossRealmTrustState, FreeIpaFinishCrossRealmTrustFlowEvent>()
                    .defaultFailureEvent(FINISH_CROSS_REALM_TRUST_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(ADD_TRUST_STATE)
                    .event(FINISH_CROSS_REALM_TRUST_EVENT)
                    .defaultFailureEvent()

                    .from(ADD_TRUST_STATE)
                    .to(FINISH_CROSS_REALM_TRUST_FINISHED_STATE)
                    .event(ADD_TRUST_FINISHED_EVENT)
                    .failureEvent(ADD_TRUST_FAILED_EVENT)

                    .from(FINISH_CROSS_REALM_TRUST_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINISH_CROSS_REALM_TRUST_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaFinishCrossRealmTrustState, FreeIpaFinishCrossRealmTrustFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, FINISH_CROSS_REALM_TRUST_FAILED_STATE, FINISH_CROSS_REALM_TRUST_FAILURE_HANDLED_EVENT);

    public FreeIpaFinishCrossRealmTrustFlowConfig() {
        super(FreeIpaFinishCrossRealmTrustState.class, FreeIpaFinishCrossRealmTrustFlowEvent.class);
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (FreeIpaPrepareCrossRealmTrustState.INIT_STATE.equals(flowState)) {
            return FINISH_CROSS_REALM_TRUST_STARTED;
        } else if (FINISH_CROSS_REALM_TRUST_FINISHED_STATE.equals(flowState)) {
            return FINISH_CROSS_REALM_TRUST_FINISHED;
        } else if (FINISH_CROSS_REALM_TRUST_FAILED_STATE.equals(flowState)) {
            return FINISH_CROSS_REALM_TRUST_FAILED;
        } else {
            return UNSET;
        }
    }

    @Override
    protected List<Transition<FreeIpaFinishCrossRealmTrustState, FreeIpaFinishCrossRealmTrustFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaFinishCrossRealmTrustState, FreeIpaFinishCrossRealmTrustFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaFinishCrossRealmTrustFlowEvent[] getEvents() {
        return FreeIpaFinishCrossRealmTrustFlowEvent.values();
    }

    @Override
    public FreeIpaFinishCrossRealmTrustFlowEvent[] getInitEvents() {
        return new FreeIpaFinishCrossRealmTrustFlowEvent[]{FINISH_CROSS_REALM_TRUST_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Finish cross-realm trust";
    }

    @Override
    public FreeIpaFinishCrossRealmTrustFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
