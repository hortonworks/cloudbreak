package com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_POLLING_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_POLLING_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartState.CLUSTER_SERVICE_RESTARTING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartState.CLUSTER_SERVICE_RESTART_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartState.CLUSTER_SERVICE_RESTART_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartState.CLUSTER_SERVICE_RESTART_POLLING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartState.INIT_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterServicesRestartFlowConfig extends AbstractFlowConfiguration<ClusterServicesRestartState, ClusterServicesRestartEvent>
        implements RetryableFlowConfiguration<ClusterServicesRestartEvent> {

    private static final List<Transition<ClusterServicesRestartState, ClusterServicesRestartEvent>> TRANSITIONS =
            new Builder<ClusterServicesRestartState, ClusterServicesRestartEvent>()
                    .from(INIT_STATE).to(CLUSTER_SERVICE_RESTARTING_STATE)
                    .event(CLUSTER_SERVICES_RESTART_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(CLUSTER_SERVICE_RESTARTING_STATE).to(CLUSTER_SERVICE_RESTART_POLLING_STATE)
                    .event(CLUSTER_SERVICES_RESTART_POLLING_EVENT)
                    .failureEvent(CLUSTER_SERVICES_RESTART_FAILURE_EVENT)

                    .from(CLUSTER_SERVICE_RESTART_POLLING_STATE).to(CLUSTER_SERVICE_RESTART_FINISHED_STATE)
                    .event(CLUSTER_SERVICES_RESTART_POLLING_FINISHED_EVENT)
                    .failureEvent(CLUSTER_SERVICES_RESTART_POLLING_EVENT)

                    .from(CLUSTER_SERVICE_RESTART_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ClusterServicesRestartState, ClusterServicesRestartEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_SERVICE_RESTART_FAILED_STATE, FAIL_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public ClusterServicesRestartFlowConfig() {
        super(ClusterServicesRestartState.class, ClusterServicesRestartEvent.class);
    }

    @Override
    protected List<Transition<ClusterServicesRestartState, ClusterServicesRestartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterServicesRestartState, ClusterServicesRestartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterServicesRestartEvent[] getEvents() {
        return ClusterServicesRestartEvent.values();
    }

    @Override
    public ClusterServicesRestartEvent[] getInitEvents() {
        return new ClusterServicesRestartEvent[]{
                CLUSTER_SERVICES_RESTART_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Start cluster";
    }

    @Override
    public ClusterServicesRestartEvent getRetryableEvent() {
        return FAIL_HANDLED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
