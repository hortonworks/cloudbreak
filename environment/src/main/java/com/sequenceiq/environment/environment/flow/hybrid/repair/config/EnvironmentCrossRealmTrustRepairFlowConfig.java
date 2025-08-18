package com.sequenceiq.environment.environment.flow.hybrid.repair.config;

import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.TRUST_REPAIR_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.TRUST_REPAIR_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.TRUST_REPAIR_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.TRUST_REPAIR_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.FAILED_TRUST_REPAIR_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.FINALIZE_TRUST_REPAIR_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.FINISH_TRUST_REPAIR_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.HANDLED_FAILED_TRUST_REPAIR_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.TRUST_REPAIR_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.TRUST_REPAIR_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvironmentCrossRealmTrustRepairFlowConfig
        extends AbstractFlowConfiguration<EnvironmentCrossRealmTrustRepairState, EnvironmentCrossRealmTrustRepairStateSelectors>
        implements RetryableFlowConfiguration<EnvironmentCrossRealmTrustRepairStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvironmentCrossRealmTrustRepairState, EnvironmentCrossRealmTrustRepairStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnvironmentCrossRealmTrustRepairState, EnvironmentCrossRealmTrustRepairStateSelectors>()
            .defaultFailureEvent(FAILED_TRUST_REPAIR_EVENT)

            .from(INIT_STATE)
            .to(TRUST_REPAIR_VALIDATION_STATE)
            .event(TRUST_REPAIR_VALIDATION_EVENT)
            .defaultFailureEvent()

            .from(TRUST_REPAIR_VALIDATION_STATE)
            .to(TRUST_REPAIR_STATE)
            .event(TRUST_REPAIR_EVENT)
            .defaultFailureEvent()

            .from(TRUST_REPAIR_STATE)
            .to(TRUST_REPAIR_FINISHED_STATE)
            .event(FINISH_TRUST_REPAIR_EVENT)
            .defaultFailureEvent()

            .from(TRUST_REPAIR_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_TRUST_REPAIR_EVENT)
            .defaultFailureEvent()

            .build();

    protected EnvironmentCrossRealmTrustRepairFlowConfig() {
        super(EnvironmentCrossRealmTrustRepairState.class, EnvironmentCrossRealmTrustRepairStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvironmentCrossRealmTrustRepairState, EnvironmentCrossRealmTrustRepairStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvironmentCrossRealmTrustRepairState, EnvironmentCrossRealmTrustRepairStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                TRUST_REPAIR_FAILED_STATE,
                HANDLED_FAILED_TRUST_REPAIR_EVENT
        );
    }

    @Override
    public EnvironmentCrossRealmTrustRepairStateSelectors[] getEvents() {
        return EnvironmentCrossRealmTrustRepairStateSelectors.values();
    }

    @Override
    public EnvironmentCrossRealmTrustRepairStateSelectors[] getInitEvents() {
        return new EnvironmentCrossRealmTrustRepairStateSelectors[] {TRUST_REPAIR_VALIDATION_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Cross Realm Trust repair";
    }

    @Override
    public EnvironmentCrossRealmTrustRepairStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_TRUST_REPAIR_EVENT;
    }

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        return UsageProto.CDPEnvironmentStatus.Value.UNSET;
    }
}
