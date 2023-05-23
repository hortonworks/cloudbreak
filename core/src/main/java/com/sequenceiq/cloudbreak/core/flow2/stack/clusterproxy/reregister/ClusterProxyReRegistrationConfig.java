package com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister;

import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent.CLUSTER_PROXY_CCMV1_REMAP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent.CLUSTER_PROXY_CCMV1_REMAP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent.CLUSTER_PROXY_CCMV1_REMAP_FINISHED_SKIP_RE_REGISTRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent.CLUSTER_PROXY_RE_REGISTRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent.CLUSTER_PROXY_RE_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent.CLUSTER_PROXY_RE_REGISTRATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent.CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationState.CLUSTER_PROXY_CCMV1_REMAP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationState.CLUSTER_PROXY_RE_REGISTRATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationState.CLUSTER_PROXY_RE_REGISTRATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterProxyReRegistrationConfig
        extends StackStatusFinalizerAbstractFlowConfig<ClusterProxyReRegistrationState, ClusterProxyReRegistrationEvent>
        implements RetryableFlowConfiguration<ClusterProxyReRegistrationEvent> {
    private static final List<Transition<ClusterProxyReRegistrationState, ClusterProxyReRegistrationEvent>> TRANSITIONS =
            new Builder<ClusterProxyReRegistrationState, ClusterProxyReRegistrationEvent>()
                    .defaultFailureEvent(CLUSTER_PROXY_RE_REGISTRATION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(CLUSTER_PROXY_RE_REGISTRATION_STATE)
                    .event(CLUSTER_PROXY_RE_REGISTRATION_EVENT)
                    .noFailureEvent()

                    .from(INIT_STATE)
                    .to(CLUSTER_PROXY_CCMV1_REMAP_STATE)
                    .event(CLUSTER_PROXY_CCMV1_REMAP_EVENT)
                    .noFailureEvent()

                    .from(CLUSTER_PROXY_CCMV1_REMAP_STATE)
                    .to(CLUSTER_PROXY_RE_REGISTRATION_STATE)
                    .event(CLUSTER_PROXY_CCMV1_REMAP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_PROXY_RE_REGISTRATION_STATE)
                    .to(FINAL_STATE)
                    .event(CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_PROXY_CCMV1_REMAP_STATE)
                    .to(FINAL_STATE)
                    .event(CLUSTER_PROXY_CCMV1_REMAP_FINISHED_SKIP_RE_REGISTRATION_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ClusterProxyReRegistrationState, ClusterProxyReRegistrationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CLUSTER_PROXY_RE_REGISTRATION_FAILED_STATE, CLUSTER_PROXY_RE_REGISTRATION_FAIL_HANDLED_EVENT);

    public ClusterProxyReRegistrationConfig() {
        super(ClusterProxyReRegistrationState.class, ClusterProxyReRegistrationEvent.class);
    }

    @Override
    protected List<Transition<ClusterProxyReRegistrationState, ClusterProxyReRegistrationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterProxyReRegistrationState, ClusterProxyReRegistrationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterProxyReRegistrationEvent[] getEvents() {
        return ClusterProxyReRegistrationEvent.values();
    }

    @Override
    public ClusterProxyReRegistrationEvent[] getInitEvents() {
        return new ClusterProxyReRegistrationEvent[]{
                CLUSTER_PROXY_RE_REGISTRATION_EVENT,
                CLUSTER_PROXY_CCMV1_REMAP_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Re register cluster proxy config";
    }

    @Override
    public ClusterProxyReRegistrationEvent getRetryableEvent() {
        return CLUSTER_PROXY_RE_REGISTRATION_FAIL_HANDLED_EVENT;
    }
}
