package com.sequenceiq.environment.environment.flow.start.config;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.RESUME_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.RESUME_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.RESUME_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;
import static com.sequenceiq.environment.environment.flow.start.EnvStartState.ENV_START_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.start.EnvStartState.ENV_START_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.start.EnvStartState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.start.EnvStartState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.start.EnvStartState.START_DATAHUB_STATE;
import static com.sequenceiq.environment.environment.flow.start.EnvStartState.START_DATALAKE_STATE;
import static com.sequenceiq.environment.environment.flow.start.EnvStartState.START_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.start.EnvStartState.SYNCHRONIZE_USERS_STATE;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.ENV_START_DATAHUB_EVENT;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.ENV_START_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.ENV_START_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.ENV_START_SYNCHRONIZE_USERS_EVENT;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.FAILED_ENV_START_EVENT;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.FINALIZE_ENV_START_EVENT;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.FINISH_ENV_START_EVENT;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.HANDLED_FAILED_ENV_START_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.start.EnvStartState;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvStartFlowConfig extends AbstractFlowConfiguration<EnvStartState, EnvStartStateSelectors>
        implements RetryableFlowConfiguration<EnvStartStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvStartState, EnvStartStateSelectors>> TRANSITIONS = new Transition.Builder<EnvStartState, EnvStartStateSelectors>()
            .defaultFailureEvent(FAILED_ENV_START_EVENT)

            .from(INIT_STATE).to(START_FREEIPA_STATE)
            .event(ENV_START_FREEIPA_EVENT).defaultFailureEvent()

            .from(START_FREEIPA_STATE).to(START_DATALAKE_STATE)
            .event(ENV_START_DATALAKE_EVENT).defaultFailureEvent()

            .from(START_DATALAKE_STATE).to(START_DATAHUB_STATE)
            .event(ENV_START_DATAHUB_EVENT).defaultFailureEvent()

            .from(START_DATAHUB_STATE).to(SYNCHRONIZE_USERS_STATE)
            .event(ENV_START_SYNCHRONIZE_USERS_EVENT).defaultFailureEvent()

            .from(SYNCHRONIZE_USERS_STATE).to(ENV_START_FINISHED_STATE)
            .event(FINISH_ENV_START_EVENT).defaultFailureEvent()

            .from(ENV_START_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_ENV_START_EVENT).defaultFailureEvent()

            .build();

    protected EnvStartFlowConfig() {
        super(EnvStartState.class, EnvStartStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvStartState, EnvStartStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<EnvStartState, EnvStartStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENV_START_FAILED_STATE, HANDLED_FAILED_ENV_START_EVENT);
    }

    @Override
    public EnvStartStateSelectors[] getEvents() {
        return EnvStartStateSelectors.values();
    }

    @Override
    public EnvStartStateSelectors[] getInitEvents() {
        return new EnvStartStateSelectors[]{ENV_START_FREEIPA_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Start environment";
    }

    @Override
    public EnvStartStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_ENV_START_EVENT;
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return RESUME_STARTED;
        } else if (ENV_START_FINISHED_STATE.equals(flowState)) {
            return RESUME_FINISHED;
        } else if (ENV_START_FAILED_STATE.equals(flowState)) {
            return RESUME_FAILED;
        } else {
            return UNSET;
        }
    }
}
