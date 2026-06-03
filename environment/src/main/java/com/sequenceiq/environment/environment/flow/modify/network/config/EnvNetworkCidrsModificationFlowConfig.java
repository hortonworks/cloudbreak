package com.sequenceiq.environment.environment.flow.modify.network.config;

import static com.sequenceiq.environment.environment.flow.modify.network.EnvNetworkCidrsModificationState.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_START_STATE;
import static com.sequenceiq.environment.environment.flow.modify.network.EnvNetworkCidrsModificationState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.modify.network.EnvNetworkCidrsModificationState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.modify.network.EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_DATALAKE_AND_DATAHUBS_STATE;
import static com.sequenceiq.environment.environment.flow.modify.network.EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.modify.network.EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.modify.network.EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FAILED_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FINALIZE_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FINISH_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.HANDLED_FAILED_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_ENVIRONMENT_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_NETWORK_CIDRS_FREEIPA_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.modify.network.EnvNetworkCidrsModificationState;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvNetworkCidrsModificationFlowConfig extends
        AbstractFlowConfiguration<EnvNetworkCidrsModificationState, EnvNetworkCidrsModificationStateSelectors>
        implements RetryableFlowConfiguration<EnvNetworkCidrsModificationStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvNetworkCidrsModificationState, EnvNetworkCidrsModificationStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnvNetworkCidrsModificationState, EnvNetworkCidrsModificationStateSelectors>()
                    .defaultFailureEvent(FAILED_MODIFY_NETWORK_CIDRS_EVENT)

                    .from(INIT_STATE).to(ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_START_STATE)
                    .event(START_MODIFY_ENVIRONMENT_NETWORK_CIDRS_EVENT).defaultFailureEvent()

                    .from(ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_START_STATE).to(NETWORK_CIDRS_MODIFICATION_FREEIPA_STATE)
                    .event(START_MODIFY_NETWORK_CIDRS_FREEIPA_EVENT).defaultFailureEvent()

                    .from(NETWORK_CIDRS_MODIFICATION_FREEIPA_STATE).to(NETWORK_CIDRS_MODIFICATION_DATALAKE_AND_DATAHUBS_STATE)
                    .event(START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT).defaultFailureEvent()

                    .from(NETWORK_CIDRS_MODIFICATION_DATALAKE_AND_DATAHUBS_STATE).to(NETWORK_CIDRS_MODIFICATION_FINISHED_STATE)
                    .event(FINISH_MODIFY_NETWORK_CIDRS_EVENT).defaultFailureEvent()

                    .from(NETWORK_CIDRS_MODIFICATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_MODIFY_NETWORK_CIDRS_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<EnvNetworkCidrsModificationState, EnvNetworkCidrsModificationStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, NETWORK_CIDRS_MODIFICATION_FAILED_STATE, HANDLED_FAILED_MODIFY_NETWORK_CIDRS_EVENT);

    protected EnvNetworkCidrsModificationFlowConfig() {
        super(EnvNetworkCidrsModificationState.class, EnvNetworkCidrsModificationStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvNetworkCidrsModificationState, EnvNetworkCidrsModificationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvNetworkCidrsModificationState, EnvNetworkCidrsModificationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public EnvNetworkCidrsModificationStateSelectors[] getEvents() {
        return EnvNetworkCidrsModificationStateSelectors.values();
    }

    @Override
    public EnvNetworkCidrsModificationStateSelectors[] getInitEvents() {
        return new EnvNetworkCidrsModificationStateSelectors[]{START_MODIFY_ENVIRONMENT_NETWORK_CIDRS_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Modify environment network CIDRs";
    }

    @Override
    public EnvNetworkCidrsModificationStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MODIFY_NETWORK_CIDRS_EVENT;
    }

    @Override
    public UsageProto.CDPEnvironmentStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        return null;
    }
}
