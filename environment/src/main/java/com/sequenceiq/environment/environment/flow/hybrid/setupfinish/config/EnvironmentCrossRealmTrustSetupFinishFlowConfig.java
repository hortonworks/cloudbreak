package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.config;


import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.FAILED_TRUST_SETUP_FINISH_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.FINALIZE_TRUST_SETUP_FINISH_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.FINISH_TRUST_SETUP_FINISH_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.TRUST_SETUP_FINISH_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.TRUST_SETUP_FINISH_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.EnvironmentCrossRealmTrustSetupFinishState;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvironmentCrossRealmTrustSetupFinishFlowConfig extends AbstractFlowConfiguration
        <EnvironmentCrossRealmTrustSetupFinishState, EnvironmentCrossRealmTrustSetupFinishStateSelectors>
        implements RetryableFlowConfiguration<EnvironmentCrossRealmTrustSetupFinishStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvironmentCrossRealmTrustSetupFinishState, EnvironmentCrossRealmTrustSetupFinishStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnvironmentCrossRealmTrustSetupFinishState, EnvironmentCrossRealmTrustSetupFinishStateSelectors>()
            .defaultFailureEvent(FAILED_TRUST_SETUP_FINISH_EVENT)

            .from(EnvironmentCrossRealmTrustSetupFinishState.INIT_STATE)
            .to(EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_VALIDATION_STATE)
            .event(TRUST_SETUP_FINISH_VALIDATION_EVENT)
            .defaultFailureEvent()

            .from(EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_VALIDATION_STATE)
            .to(EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_STATE)
            .event(TRUST_SETUP_FINISH_EVENT)
            .defaultFailureEvent()

            .from(EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_STATE)
            .to(EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_FINISHED_STATE)
            .event(FINISH_TRUST_SETUP_FINISH_EVENT)
            .defaultFailureEvent()

            .from(EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_FINISHED_STATE)
            .to(EnvironmentCrossRealmTrustSetupFinishState.FINAL_STATE)
            .event(FINALIZE_TRUST_SETUP_FINISH_EVENT)
            .defaultFailureEvent()

            .build();

    protected EnvironmentCrossRealmTrustSetupFinishFlowConfig() {
        super(EnvironmentCrossRealmTrustSetupFinishState.class, EnvironmentCrossRealmTrustSetupFinishStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvironmentCrossRealmTrustSetupFinishState, EnvironmentCrossRealmTrustSetupFinishStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvironmentCrossRealmTrustSetupFinishState, EnvironmentCrossRealmTrustSetupFinishStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                EnvironmentCrossRealmTrustSetupFinishState.INIT_STATE,
                EnvironmentCrossRealmTrustSetupFinishState.FINAL_STATE,
                EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_FAILED_STATE,
                EnvironmentCrossRealmTrustSetupFinishStateSelectors.HANDLED_FAILED_TRUST_SETUP_FINISH_EVENT
        );
    }

    @Override
    public EnvironmentCrossRealmTrustSetupFinishStateSelectors[] getEvents() {
        return EnvironmentCrossRealmTrustSetupFinishStateSelectors.values();
    }

    @Override
    public EnvironmentCrossRealmTrustSetupFinishStateSelectors[] getInitEvents() {
        return new EnvironmentCrossRealmTrustSetupFinishStateSelectors[] {
                EnvironmentCrossRealmTrustSetupFinishStateSelectors.TRUST_SETUP_FINISH_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Cross Realm Trust Setup Finish";
    }

    @Override
    public EnvironmentCrossRealmTrustSetupFinishStateSelectors getRetryableEvent() {
        return EnvironmentCrossRealmTrustSetupFinishStateSelectors.HANDLED_FAILED_TRUST_SETUP_FINISH_EVENT;
    }

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        return UsageProto.CDPEnvironmentStatus.Value.UNSET;
    }
}
