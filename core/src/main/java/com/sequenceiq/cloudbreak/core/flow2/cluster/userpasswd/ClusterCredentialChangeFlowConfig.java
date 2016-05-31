package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.CLUSTER_CREDENTIALCHANGE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.CLUSTER_CREDENTIALCHANGE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.CLUSTER_CREDENTIALCHANGE_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.CLUSTER_CREDENTIALCHANGE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.CLUSTER_CREDENTIALCHANGE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.CLUSTER_CREDENTIALCHANGE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class ClusterCredentialChangeFlowConfig extends AbstractFlowConfiguration<ClusterCredentialChangeState, ClusterCredentialChangeEvent> {
    private static final List<Transition<ClusterCredentialChangeState, ClusterCredentialChangeEvent>> TRANSITIONS =
            new Transition.Builder<ClusterCredentialChangeState, ClusterCredentialChangeEvent>()
                    .from(INIT_STATE).to(CLUSTER_CREDENTIALCHANGE_STATE).event(CLUSTER_CREDENTIALCHANGE_EVENT).noFailureEvent()
                    .from(CLUSTER_CREDENTIALCHANGE_STATE).to(CLUSTER_CREDENTIALCHANGE_FINISHED_STATE).event(CLUSTER_CREDENTIALCHANGE_FINISHED_EVENT)
                            .failureEvent(CLUSTER_CREDENTIALCHANGE_FINISHED_FAILURE_EVENT)
                    .from(CLUSTER_CREDENTIALCHANGE_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).failureEvent(FAILURE_EVENT)
                    .build();

    private static final FlowEdgeConfig<ClusterCredentialChangeState, ClusterCredentialChangeEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_CREDENTIALCHANGE_FAILED_STATE, FAIL_HANDLED_EVENT);

    public ClusterCredentialChangeFlowConfig() {
        super(ClusterCredentialChangeState.class, ClusterCredentialChangeEvent.class);
    }

    @Override
    protected List<Transition<ClusterCredentialChangeState, ClusterCredentialChangeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterCredentialChangeState, ClusterCredentialChangeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterCredentialChangeEvent[] getEvents() {
        return ClusterCredentialChangeEvent.values();
    }

    @Override
    public ClusterCredentialChangeEvent[] getInitEvents() {
        return new ClusterCredentialChangeEvent[] {
                CLUSTER_CREDENTIALCHANGE_EVENT
        };
    }
}
