package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_ADD_CONTAINERS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_ADD_CONTAINERS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_EXECUTE_POST_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_EXECUTE_PRE_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_INSTALL_AMBARI_NODES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_INSTALL_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_INSTALl_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_SSSD_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.ADD_CLUSTER_CONTAINERS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CONFIGURE_SSSD_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.EXECUTE_POST_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.EXECUTE_PRE_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINALIZE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INSTALL_AMBARI_NODES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INSTALL_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INSTALL_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class ClusterUpscaleFlowConfig extends AbstractFlowConfiguration<ClusterUpscaleState, ClusterUpscaleEvent> {

    private static final List<Transition<ClusterUpscaleState, ClusterUpscaleEvent>> TRANSITIONS =
            new Transition.Builder<ClusterUpscaleState, ClusterUpscaleEvent>()
                .defaultFailureEvent(CLUSTER_UPSCALE_FAILURE_EVENT)
                .from(INIT_STATE).to(ADD_CLUSTER_CONTAINERS_STATE).event(CLUSTER_UPSCALE_ADD_CONTAINERS_EVENT).defaultFailureEvent()
                .from(ADD_CLUSTER_CONTAINERS_STATE).to(INSTALL_AMBARI_NODES_STATE).event(CLUSTER_UPSCALE_ADD_CONTAINERS_FINISHED_EVENT).defaultFailureEvent()
                .from(INSTALL_AMBARI_NODES_STATE).to(CONFIGURE_SSSD_STATE).event(CLUSTER_UPSCALE_INSTALL_AMBARI_NODES_FINISHED_EVENT).defaultFailureEvent()
                .from(CONFIGURE_SSSD_STATE).to(INSTALL_RECIPES_STATE).event(CLUSTER_UPSCALE_SSSD_CONFIG_FINISHED_EVENT).defaultFailureEvent()
                .from(INSTALL_RECIPES_STATE).to(EXECUTE_PRE_RECIPES_STATE).event(CLUSTER_UPSCALE_INSTALL_RECIPES_FINISHED_EVENT).defaultFailureEvent()
                .from(EXECUTE_PRE_RECIPES_STATE).to(INSTALL_SERVICES_STATE).event(CLUSTER_UPSCALE_EXECUTE_PRE_RECIPES_FINISHED_EVENT).defaultFailureEvent()
                .from(INSTALL_SERVICES_STATE).to(EXECUTE_POST_RECIPES_STATE).event(CLUSTER_UPSCALE_INSTALl_SERVICES_FINISHED_EVENT).defaultFailureEvent()
                .from(EXECUTE_POST_RECIPES_STATE).to(FINALIZE_STATE).event(CLUSTER_UPSCALE_EXECUTE_POST_RECIPES_FINISHED_EVENT).defaultFailureEvent()
                .build();

    private static final FlowEdgeConfig<ClusterUpscaleState, ClusterUpscaleEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            FINALIZE_STATE, CLUSTER_UPSCALE_FINALIZED_EVENT, FAILED_STATE, CLUSTER_UPSCALE_FAIL_HANDLED_EVENT);

    public ClusterUpscaleFlowConfig() {
        super(ClusterUpscaleState.class, ClusterUpscaleEvent.class);
    }

    @Override
    protected List<Transition<ClusterUpscaleState, ClusterUpscaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterUpscaleState, ClusterUpscaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterUpscaleEvent[] getEvents() {
        return ClusterUpscaleEvent.values();
    }
}
