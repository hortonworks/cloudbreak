package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.ADD_CONTAINERS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.ADD_CONTAINERS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.ADD_CONTAINERS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_POST_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_POST_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_PRE_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_PRE_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.INSTALL_FS_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.INSTALL_FS_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.INSTALL_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.INSTALL_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.INSTALL_SERVICES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.INSTALL_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.SSSD_CONFIG_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.SSSD_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPDATE_METADATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPDATE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.WAIT_FOR_AMBARI_HOSTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.WAIT_FOR_AMBARI_HOSTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.ADD_CLUSTER_CONTAINERS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CONFIGURE_SSSD_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.EXECUTE_POST_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.EXECUTE_PRE_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINALIZE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INSTALL_FS_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INSTALL_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INSTALL_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPDATE_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.WAIT_FOR_AMBARI_HOSTS_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class ClusterUpscaleFlowConfig extends AbstractFlowConfiguration<ClusterUpscaleState, ClusterUpscaleEvent> {

    private static final List<Transition<ClusterUpscaleState, ClusterUpscaleEvent>> TRANSITIONS =
            new Transition.Builder<ClusterUpscaleState, ClusterUpscaleEvent>()
                .defaultFailureEvent(FAILURE_EVENT)
                .from(INIT_STATE).to(ADD_CLUSTER_CONTAINERS_STATE).event(ADD_CONTAINERS_EVENT).noFailureEvent()
                .from(ADD_CLUSTER_CONTAINERS_STATE).to(INSTALL_FS_RECIPES_STATE).event(ADD_CONTAINERS_FINISHED_EVENT).failureEvent(ADD_CONTAINERS_FAILED_EVENT)
                .from(INSTALL_FS_RECIPES_STATE).to(WAIT_FOR_AMBARI_HOSTS_STATE).event(INSTALL_FS_RECIPES_FINISHED_EVENT)
                    .failureEvent(INSTALL_FS_RECIPES_FAILED_EVENT)
                .from(WAIT_FOR_AMBARI_HOSTS_STATE).to(CONFIGURE_SSSD_STATE).event(WAIT_FOR_AMBARI_HOSTS_FINISHED_EVENT)
                    .failureEvent(WAIT_FOR_AMBARI_HOSTS_FAILED_EVENT)
                .from(CONFIGURE_SSSD_STATE).to(INSTALL_RECIPES_STATE).event(SSSD_CONFIG_FINISHED_EVENT)
                    .failureEvent(SSSD_CONFIG_FAILED_EVENT)
                .from(INSTALL_RECIPES_STATE).to(EXECUTE_PRE_RECIPES_STATE).event(INSTALL_RECIPES_FINISHED_EVENT)
                    .failureEvent(INSTALL_RECIPES_FAILED_EVENT)
                .from(EXECUTE_PRE_RECIPES_STATE).to(INSTALL_SERVICES_STATE).event(EXECUTE_PRE_RECIPES_FINISHED_EVENT)
                    .failureEvent(EXECUTE_PRE_RECIPES_FAILED_EVENT)
                .from(INSTALL_SERVICES_STATE).to(EXECUTE_POST_RECIPES_STATE).event(INSTALL_SERVICES_FINISHED_EVENT)
                    .failureEvent(INSTALL_SERVICES_FAILED_EVENT)
                .from(EXECUTE_POST_RECIPES_STATE).to(UPDATE_METADATA_STATE).event(EXECUTE_POST_RECIPES_FINISHED_EVENT)
                    .failureEvent(EXECUTE_POST_RECIPES_FAILED_EVENT)
                .from(UPDATE_METADATA_STATE).to(FINALIZE_STATE).event(UPDATE_METADATA_FINISHED_EVENT)
                    .failureEvent(UPDATE_METADATA_FAILED_EVENT)
                .from(FINALIZE_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).defaultFailureEvent()
                .build();

    private static final FlowEdgeConfig<ClusterUpscaleState, ClusterUpscaleEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            FAILED_STATE, FAIL_HANDLED_EVENT);

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

    @Override
    public ClusterUpscaleEvent[] getInitEvents() {
        return new ClusterUpscaleEvent[]{ ClusterUpscaleEvent.ADD_CONTAINERS_EVENT };
    }
}
