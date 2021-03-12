package com.sequenceiq.environment.environment.flow.deletion.config;

import static com.sequenceiq.environment.environment.flow.deletion.EnvClustersDeleteState.DATAHUB_CLUSTERS_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvClustersDeleteState.DATALAKE_CLUSTERS_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvClustersDeleteState.ENV_CLUSTERS_DELETE_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvClustersDeleteState.ENV_CLUSTERS_DELETE_FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvClustersDeleteState.ENV_CLUSTERS_DELETE_INIT_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvClustersDeleteState.EXPERIENCE_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.FINISH_ENV_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.HANDLED_FAILED_ENV_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.START_DATAHUB_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.START_DATALAKE_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.START_EXPERIENCE_DELETE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.flow.deletion.EnvClustersDeleteState;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
// TODO: CB-11559
public class EnvClustersDeleteFlowConfig extends AbstractFlowConfiguration<EnvClustersDeleteState, EnvClustersDeleteStateSelectors>
        implements RetryableFlowConfiguration<EnvClustersDeleteStateSelectors> {

    private static final List<Transition<EnvClustersDeleteState, EnvClustersDeleteStateSelectors>> TRANSITIONS =
            new Builder<EnvClustersDeleteState, EnvClustersDeleteStateSelectors>()
                    .defaultFailureEvent(EnvClustersDeleteStateSelectors.FAILED_ENV_CLUSTERS_DELETE_EVENT)

            .from(ENV_CLUSTERS_DELETE_INIT_STATE).to(DATAHUB_CLUSTERS_DELETE_STARTED_STATE)
            .event(START_DATAHUB_CLUSTERS_DELETE_EVENT).defaultFailureEvent()

            .from(DATAHUB_CLUSTERS_DELETE_STARTED_STATE).to(EXPERIENCE_DELETE_STARTED_STATE)
            .event(START_EXPERIENCE_DELETE_EVENT).defaultFailureEvent()

            .from(EXPERIENCE_DELETE_STARTED_STATE).to(DATALAKE_CLUSTERS_DELETE_STARTED_STATE)
            .event(START_DATALAKE_CLUSTERS_DELETE_EVENT).defaultFailureEvent()

            .from(DATALAKE_CLUSTERS_DELETE_STARTED_STATE).to(ENV_CLUSTERS_DELETE_FINAL_STATE)
            .event(FINISH_ENV_CLUSTERS_DELETE_EVENT).defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<EnvClustersDeleteState, EnvClustersDeleteStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(ENV_CLUSTERS_DELETE_INIT_STATE, ENV_CLUSTERS_DELETE_FINAL_STATE,
                    ENV_CLUSTERS_DELETE_FAILED_STATE, HANDLED_FAILED_ENV_CLUSTERS_DELETE_EVENT);

    public EnvClustersDeleteFlowConfig() {
        super(EnvClustersDeleteState.class, EnvClustersDeleteStateSelectors.class);
    }

    @Override
    public EnvClustersDeleteStateSelectors[] getEvents() {
        return EnvClustersDeleteStateSelectors.values();
    }

    @Override
    public EnvClustersDeleteStateSelectors[] getInitEvents() {
        return new EnvClustersDeleteStateSelectors[]{
                START_DATAHUB_CLUSTERS_DELETE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Delete clusters in environment";
    }

    @Override
    public EnvClustersDeleteStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_ENV_CLUSTERS_DELETE_EVENT;
    }

    @Override
    protected List<Transition<EnvClustersDeleteState, EnvClustersDeleteStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<EnvClustersDeleteState, EnvClustersDeleteStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
