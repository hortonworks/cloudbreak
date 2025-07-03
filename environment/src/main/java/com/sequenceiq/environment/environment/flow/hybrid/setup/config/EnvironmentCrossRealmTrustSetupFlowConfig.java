package com.sequenceiq.environment.environment.flow.hybrid.setup.config;


import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.FAILED_TRUST_SETUP_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.FINALIZE_TRUST_SETUP_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.FINISH_TRUST_SETUP_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.HANDLED_FAILED_TRUST_SETUP_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.TRUST_SETUP_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.TRUST_SETUP_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvironmentCrossRealmTrustSetupFlowConfig extends AbstractFlowConfiguration<EnvironmentCrossRealmTrustSetupState,
        EnvironmentCrossRealmTrustSetupStateSelectors>
        implements RetryableFlowConfiguration<EnvironmentCrossRealmTrustSetupStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvironmentCrossRealmTrustSetupState, EnvironmentCrossRealmTrustSetupStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnvironmentCrossRealmTrustSetupState, EnvironmentCrossRealmTrustSetupStateSelectors>()
            .defaultFailureEvent(FAILED_TRUST_SETUP_EVENT)

            .from(INIT_STATE)
            .to(TRUST_SETUP_VALIDATION_STATE)
            .event(TRUST_SETUP_VALIDATION_EVENT)
            .defaultFailureEvent()

            .from(TRUST_SETUP_VALIDATION_STATE)
            .to(TRUST_SETUP_STATE)
            .event(TRUST_SETUP_EVENT)
            .defaultFailureEvent()

            .from(TRUST_SETUP_STATE)
            .to(TRUST_SETUP_FINISHED_STATE)
            .event(FINISH_TRUST_SETUP_EVENT)
            .defaultFailureEvent()

            .from(TRUST_SETUP_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_TRUST_SETUP_EVENT)
            .defaultFailureEvent()

            .build();

    protected EnvironmentCrossRealmTrustSetupFlowConfig() {
        super(EnvironmentCrossRealmTrustSetupState.class, EnvironmentCrossRealmTrustSetupStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvironmentCrossRealmTrustSetupState, EnvironmentCrossRealmTrustSetupStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvironmentCrossRealmTrustSetupState, EnvironmentCrossRealmTrustSetupStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                TRUST_SETUP_FAILED_STATE,
                HANDLED_FAILED_TRUST_SETUP_EVENT
        );
    }

    @Override
    public EnvironmentCrossRealmTrustSetupStateSelectors[] getEvents() {
        return EnvironmentCrossRealmTrustSetupStateSelectors.values();
    }

    @Override
    public EnvironmentCrossRealmTrustSetupStateSelectors[] getInitEvents() {
        return new EnvironmentCrossRealmTrustSetupStateSelectors[] {TRUST_SETUP_VALIDATION_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Cross Realm Trust setup";
    }

    @Override
    public EnvironmentCrossRealmTrustSetupStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_TRUST_SETUP_EVENT;
    }

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        return UsageProto.CDPEnvironmentStatus.Value.UNSET;
    }
}
