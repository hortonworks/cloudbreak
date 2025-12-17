package com.sequenceiq.environment.environment.flow.deletion.config;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.DELETE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.DELETE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.DELETE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.CLUSTER_DEFINITION_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.DISTRIBUTION_LIST_DELETE_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.ENV_DELETE_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.ENV_DELETE_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.EVENT_CLEANUP_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.FREEIPA_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.IDBROKER_MAPPINGS_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.NETWORK_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.PUBLICKEY_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.RDBMS_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.S3GUARD_TABLE_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.UMS_RESOURCE_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINALIZE_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.HANDLED_FAILED_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_CLUSTER_DEFINITION_CLEANUP_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_DISTRIBUTION_LIST_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_EVENT_CLEANUP_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_IDBROKER_MAPPINGS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_NETWORK_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_PUBLICKEY_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_RDBMS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_S3GUARD_TABLE_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_UMS_RESOURCE_DELETE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvDeleteFlowConfig extends AbstractFlowConfiguration<EnvDeleteState, EnvDeleteStateSelectors>
        implements RetryableFlowConfiguration<EnvDeleteStateSelectors>, EnvironmentUseCaseAware {

    private static final List<Transition<EnvDeleteState, EnvDeleteStateSelectors>> TRANSITIONS
            = new Transition.Builder<EnvDeleteState, EnvDeleteStateSelectors>().defaultFailureEvent(EnvDeleteStateSelectors.FAILED_ENV_DELETE_EVENT)

            .from(INIT_STATE).to(FREEIPA_DELETE_STARTED_STATE)
            .event(START_FREEIPA_DELETE_EVENT).defaultFailureEvent()

            // Deprecated, can be deleted from 2.81
            .from(FREEIPA_DELETE_STARTED_STATE).to(STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_STARTED_STATE)
            .event(START_STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_EVENT).defaultFailureEvent()

            // Deprecated, can be deleted from 2.81
            .from(STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_STARTED_STATE).to(RDBMS_DELETE_STARTED_STATE)
            .event(START_RDBMS_DELETE_EVENT).defaultFailureEvent()

            .from(FREEIPA_DELETE_STARTED_STATE).to(RDBMS_DELETE_STARTED_STATE)
            .event(START_RDBMS_DELETE_EVENT).defaultFailureEvent()

            .from(RDBMS_DELETE_STARTED_STATE).to(ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_STARTED_STATE)
            .event(START_ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_EVENT).defaultFailureEvent()

            .from(ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_STARTED_STATE).to(PUBLICKEY_DELETE_STARTED_STATE)
            .event(START_PUBLICKEY_DELETE_EVENT).defaultFailureEvent()

            .from(PUBLICKEY_DELETE_STARTED_STATE).to(NETWORK_DELETE_STARTED_STATE)
            .event(START_NETWORK_DELETE_EVENT).defaultFailureEvent()

            .from(NETWORK_DELETE_STARTED_STATE).to(IDBROKER_MAPPINGS_DELETE_STARTED_STATE)
            .event(START_IDBROKER_MAPPINGS_DELETE_EVENT).defaultFailureEvent()

            .from(IDBROKER_MAPPINGS_DELETE_STARTED_STATE).to(S3GUARD_TABLE_DELETE_STARTED_STATE)
            .event(START_S3GUARD_TABLE_DELETE_EVENT).defaultFailureEvent()

            .from(S3GUARD_TABLE_DELETE_STARTED_STATE).to(CLUSTER_DEFINITION_DELETE_STARTED_STATE)
            .event(START_CLUSTER_DEFINITION_CLEANUP_EVENT).defaultFailureEvent()

            .from(CLUSTER_DEFINITION_DELETE_STARTED_STATE).to(UMS_RESOURCE_DELETE_STARTED_STATE)
            .event(START_UMS_RESOURCE_DELETE_EVENT).defaultFailureEvent()

            .from(UMS_RESOURCE_DELETE_STARTED_STATE).to(EVENT_CLEANUP_STARTED_STATE)
            .event(START_EVENT_CLEANUP_EVENT).defaultFailureEvent()

            .from(EVENT_CLEANUP_STARTED_STATE).to(DISTRIBUTION_LIST_DELETE_STATE)
            .event(START_DISTRIBUTION_LIST_DELETE_EVENT).defaultFailureEvent()

            .from(DISTRIBUTION_LIST_DELETE_STATE).to(ENV_DELETE_FINISHED_STATE)
            .event(FINISH_ENV_DELETE_EVENT).defaultFailureEvent()

            .from(ENV_DELETE_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_ENV_DELETE_EVENT).defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<EnvDeleteState, EnvDeleteStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENV_DELETE_FAILED_STATE, HANDLED_FAILED_ENV_DELETE_EVENT);

    public EnvDeleteFlowConfig() {
        super(EnvDeleteState.class, EnvDeleteStateSelectors.class);
    }

    @Override
    public EnvDeleteStateSelectors[] getEvents() {
        return EnvDeleteStateSelectors.values();
    }

    @Override
    public EnvDeleteStateSelectors[] getInitEvents() {
        return new EnvDeleteStateSelectors[]{
                START_FREEIPA_DELETE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Delete environment";
    }

    @Override
    public EnvDeleteStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_ENV_DELETE_EVENT;
    }

    @Override
    protected List<Transition<EnvDeleteState, EnvDeleteStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnvDeleteState, EnvDeleteStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return DELETE_STARTED;
        } else if (ENV_DELETE_FINISHED_STATE.equals(flowState)) {
            return DELETE_FINISHED;
        } else if (ENV_DELETE_FAILED_STATE.equals(flowState)) {
            return DELETE_FAILED;
        } else {
            return UNSET;
        }
    }
}
