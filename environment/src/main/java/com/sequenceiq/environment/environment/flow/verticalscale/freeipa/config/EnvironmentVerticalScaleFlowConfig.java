package com.sequenceiq.environment.environment.flow.verticalscale.freeipa.config;

import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.EnvironmentVerticalScaleState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.EnvironmentVerticalScaleState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.FAILED_VERTICAL_SCALING_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.FINALIZE_VERTICAL_SCALING_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.FINISH_VERTICAL_SCALING_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.HANDLED_FAILED_VERTICAL_SCALING_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.VERTICAL_SCALING_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.VERTICAL_SCALING_FREEIPA_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.EnvironmentVerticalScaleState;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvironmentVerticalScaleFlowConfig extends AbstractFlowConfiguration<EnvironmentVerticalScaleState, EnvironmentVerticalScaleStateSelectors>
        implements RetryableFlowConfiguration<EnvironmentVerticalScaleStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvironmentVerticalScaleState, EnvironmentVerticalScaleStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnvironmentVerticalScaleState, EnvironmentVerticalScaleStateSelectors>()
            .defaultFailureEvent(FAILED_VERTICAL_SCALING_FREEIPA_EVENT)

            .from(INIT_STATE)
            .to(VERTICAL_SCALING_FREEIPA_VALIDATION_STATE)
            .event(VERTICAL_SCALING_FREEIPA_VALIDATION_EVENT)
            .defaultFailureEvent()

            .from(VERTICAL_SCALING_FREEIPA_VALIDATION_STATE)
            .to(VERTICAL_SCALING_FREEIPA_STATE)
            .event(VERTICAL_SCALING_FREEIPA_EVENT)
            .defaultFailureEvent()

            .from(VERTICAL_SCALING_FREEIPA_STATE)
            .to(VERTICAL_SCALING_FREEIPA_FINISHED_STATE)
            .event(FINISH_VERTICAL_SCALING_FREEIPA_EVENT)
            .defaultFailureEvent()

            .from(VERTICAL_SCALING_FREEIPA_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_VERTICAL_SCALING_FREEIPA_EVENT)
            .defaultFailureEvent()

            .build();

    protected EnvironmentVerticalScaleFlowConfig() {
        super(EnvironmentVerticalScaleState.class, EnvironmentVerticalScaleStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvironmentVerticalScaleState, EnvironmentVerticalScaleStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvironmentVerticalScaleState, EnvironmentVerticalScaleStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, VERTICAL_SCALING_FREEIPA_FAILED_STATE, HANDLED_FAILED_VERTICAL_SCALING_FREEIPA_EVENT);
    }

    @Override
    public EnvironmentVerticalScaleStateSelectors[] getEvents() {
        return EnvironmentVerticalScaleStateSelectors.values();
    }

    @Override
    public EnvironmentVerticalScaleStateSelectors[] getInitEvents() {
        return new EnvironmentVerticalScaleStateSelectors[] {VERTICAL_SCALING_FREEIPA_VALIDATION_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Vertical Scale FreeIPA";
    }

    @Override
    public EnvironmentVerticalScaleStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_VERTICAL_SCALING_FREEIPA_EVENT;
    }

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_VERTICAL_SCALE_STARTED;
        } else if (VERTICAL_SCALING_FREEIPA_FAILED_STATE.equals(flowState)) {
            return UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_VERTICAL_SCALE_FAILED;
        } else if (VERTICAL_SCALING_FREEIPA_FINISHED_STATE.equals(flowState)) {
            return UsageProto.CDPEnvironmentStatus.Value.ENVIRONMENT_VERTICAL_SCALE_FINISHED;
        }
        return UsageProto.CDPEnvironmentStatus.Value.UNSET;
    }
}
