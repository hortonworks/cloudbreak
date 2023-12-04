package com.sequenceiq.environment.environment.flow.modify.proxy.config;

import static com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_DATAHUBS_STATE;
import static com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_DATALAKE_STATE;
import static com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_START_STATE;
import static com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors.FAILED_MODIFY_PROXY_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors.FINALIZE_MODIFY_PROXY_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors.FINISH_MODIFY_PROXY_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors.HANDLE_FAILED_MODIFY_PROXY_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors.MODIFY_PROXY_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors.MODIFY_PROXY_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors.MODIFY_PROXY_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors.MODIFY_PROXY_START_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvProxyModificationFlowConfig extends
        AbstractFlowConfiguration<EnvProxyModificationState, EnvProxyModificationStateSelectors>
        implements RetryableFlowConfiguration<EnvProxyModificationStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvProxyModificationState, EnvProxyModificationStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnvProxyModificationState, EnvProxyModificationStateSelectors>()
                    .defaultFailureEvent(FAILED_MODIFY_PROXY_EVENT)

                    .from(INIT_STATE).to(PROXY_CONFIG_MODIFICATION_START_STATE)
                    .event(MODIFY_PROXY_START_EVENT).defaultFailureEvent()

                    .from(PROXY_CONFIG_MODIFICATION_START_STATE).to(PROXY_CONFIG_MODIFICATION_FREEIPA_STATE)
                    .event(MODIFY_PROXY_FREEIPA_EVENT).defaultFailureEvent()

                    .from(PROXY_CONFIG_MODIFICATION_FREEIPA_STATE).to(PROXY_CONFIG_MODIFICATION_DATALAKE_STATE)
                    .event(MODIFY_PROXY_DATALAKE_EVENT).defaultFailureEvent()

                    .from(PROXY_CONFIG_MODIFICATION_DATALAKE_STATE).to(PROXY_CONFIG_MODIFICATION_DATAHUBS_STATE)
                    .event(MODIFY_PROXY_DATAHUBS_EVENT).defaultFailureEvent()

                    .from(PROXY_CONFIG_MODIFICATION_DATAHUBS_STATE).to(PROXY_CONFIG_MODIFICATION_FINISHED_STATE)
                    .event(FINISH_MODIFY_PROXY_EVENT).defaultFailureEvent()

                    .from(PROXY_CONFIG_MODIFICATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_MODIFY_PROXY_EVENT).defaultFailureEvent()

                    .build();

    protected EnvProxyModificationFlowConfig() {
        super(EnvProxyModificationState.class, EnvProxyModificationStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvProxyModificationState, EnvProxyModificationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvProxyModificationState, EnvProxyModificationStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, PROXY_CONFIG_MODIFICATION_FAILED_STATE, HANDLE_FAILED_MODIFY_PROXY_EVENT);
    }

    @Override
    public EnvProxyModificationStateSelectors[] getEvents() {
        return EnvProxyModificationStateSelectors.values();
    }

    @Override
    public EnvProxyModificationStateSelectors[] getInitEvents() {
        return new EnvProxyModificationStateSelectors[]{MODIFY_PROXY_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Modify environment proxy configuration";
    }

    @Override
    public EnvProxyModificationStateSelectors getRetryableEvent() {
        return HANDLE_FAILED_MODIFY_PROXY_EVENT;
    }

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return UsageProto.CDPEnvironmentStatus.Value.PROXY_MODIFICATION_STARTED;
        } else if (PROXY_CONFIG_MODIFICATION_FAILED_STATE.equals(flowState)) {
            return UsageProto.CDPEnvironmentStatus.Value.PROXY_MODIFICATION_FAILED;
        } else if (PROXY_CONFIG_MODIFICATION_FINISHED_STATE.equals(flowState)) {
            return UsageProto.CDPEnvironmentStatus.Value.PROXY_MODIFICATION_FINISHED;
        }
        return UsageProto.CDPEnvironmentStatus.Value.UNSET;
    }
}
