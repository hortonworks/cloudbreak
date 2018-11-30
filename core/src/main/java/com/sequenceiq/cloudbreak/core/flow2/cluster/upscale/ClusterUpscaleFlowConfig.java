package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_ENSURE_COMPONENTS_STOPPED_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_ENSURE_COMPONENTS_STOPPED_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_GATHER_INSTALLED_COMPONENTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_GATHER_INSTALLED_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_INIT_COMPONENTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_INIT_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_INSTALL_COMPONENTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_INSTALL_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_REGENERATE_KERBEROS_KEYTABS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_REGENERATE_KERBEROS_KEYTABS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_RESTART_ALL_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_RESTART_ALL_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_START_COMPONENTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_START_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_START_SERVER_AGENT_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_START_SERVER_AGENT_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_STOP_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_STOP_SERVER_AGENT_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.AMBARI_STOP_SERVER_AGENT_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CHECK_HOST_METADATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CHECK_HOST_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_REPAIR_SINGLE_MASTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_POSTRECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_POSTRECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPLOAD_UPSCALE_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPLOAD_UPSCALE_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPSCALE_AMBARI_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPSCALE_AMBARI_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.values;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_ENSURE_COMPONENTS_ARE_STOPPED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_GATHER_INSTALLED_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_INIT_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_INSTALL_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_REGENERATE_KERBEROS_KEYTABS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_REPAIR_SINGLE_MASTER_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_RESTART_ALL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_START_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_START_SERVER_AGENT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_STOP_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.AMBARI_STOP_SERVER_AGENT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CHECK_HOST_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_UPSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.EXECUTING_POSTRECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINALIZE_UPSCALE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPLOAD_UPSCALE_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPSCALING_AMBARI_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPSCALING_CLUSTER_MANAGER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class ClusterUpscaleFlowConfig extends AbstractFlowConfiguration<ClusterUpscaleState, ClusterUpscaleEvent> {
    private static final List<Transition<ClusterUpscaleState, ClusterUpscaleEvent>> TRANSITIONS =
            new Builder<ClusterUpscaleState, ClusterUpscaleEvent>()
                    .defaultFailureEvent(FAILURE_EVENT)
                    .from(INIT_STATE).to(UPLOAD_UPSCALE_RECIPES_STATE).event(CLUSTER_UPSCALE_TRIGGER_EVENT).noFailureEvent()
                    .from(UPLOAD_UPSCALE_RECIPES_STATE).to(CHECK_HOST_METADATA_STATE).event(UPLOAD_UPSCALE_RECIPES_FINISHED_EVENT)
                        .failureEvent(UPLOAD_UPSCALE_RECIPES_FAILED_EVENT)
                    .from(CHECK_HOST_METADATA_STATE).to(UPSCALING_CLUSTER_MANAGER_STATE).event(CHECK_HOST_METADATA_FINISHED_EVENT)
                        .failureEvent(CHECK_HOST_METADATA_FAILED_EVENT)
                    .from(UPSCALING_CLUSTER_MANAGER_STATE).to(UPSCALING_AMBARI_FINISHED_STATE).event(UPSCALE_AMBARI_FINISHED_EVENT)
                        .failureEvent(UPSCALE_AMBARI_FAILED_EVENT)

                    // Pathway 1: repair single master node
                    .from(UPSCALING_AMBARI_FINISHED_STATE).to(AMBARI_GATHER_INSTALLED_COMPONENTS_STATE).event(CLUSTER_REPAIR_SINGLE_MASTER_START_EVENT)
                        .noFailureEvent()
                    .from(AMBARI_GATHER_INSTALLED_COMPONENTS_STATE).to(AMBARI_STOP_COMPONENTS_STATE).event(AMBARI_GATHER_INSTALLED_COMPONENTS_FINISHED_EVENT)
                        .failureEvent(AMBARI_GATHER_INSTALLED_COMPONENTS_FAILED_EVENT)
                    .from(AMBARI_STOP_COMPONENTS_STATE).to(AMBARI_STOP_SERVER_AGENT_STATE).event(AMBARI_STOP_COMPONENTS_FINISHED_EVENT)
                        .failureEvent(AMBARI_STOP_SERVER_AGENT_FAILED_EVENT)
                    .from(AMBARI_STOP_SERVER_AGENT_STATE).to(AMBARI_START_SERVER_AGENT_STATE).event(AMBARI_STOP_SERVER_AGENT_FINISHED_EVENT)
                        .failureEvent(AMBARI_STOP_SERVER_AGENT_FAILED_EVENT)
                    .from(AMBARI_START_SERVER_AGENT_STATE).to(AMBARI_REGENERATE_KERBEROS_KEYTABS_STATE).event(AMBARI_START_SERVER_AGENT_FINISHED_EVENT)
                        .failureEvent(AMBARI_START_SERVER_AGENT_FAILED_EVENT)
                    .from(AMBARI_REGENERATE_KERBEROS_KEYTABS_STATE).to(AMBARI_ENSURE_COMPONENTS_ARE_STOPPED_STATE)
                        .event(AMBARI_REGENERATE_KERBEROS_KEYTABS_FINISHED_EVENT).failureEvent(AMBARI_REGENERATE_KERBEROS_KEYTABS_FAILED_EVENT)
                    .from(AMBARI_ENSURE_COMPONENTS_ARE_STOPPED_STATE).to(AMBARI_INIT_COMPONENTS_STATE).event(AMBARI_ENSURE_COMPONENTS_STOPPED_FINISHED_EVENT)
                        .failureEvent(AMBARI_ENSURE_COMPONENTS_STOPPED_FAILED_EVENT)
                    .from(AMBARI_INIT_COMPONENTS_STATE).to(AMBARI_INSTALL_COMPONENTS_STATE).event(AMBARI_INIT_COMPONENTS_FINISHED_EVENT)
                        .failureEvent(AMBARI_INIT_COMPONENTS_FAILED_EVENT)
                    .from(AMBARI_INSTALL_COMPONENTS_STATE).to(AMBARI_START_COMPONENTS_STATE).event(AMBARI_INSTALL_COMPONENTS_FINISHED_EVENT)
                        .failureEvent(AMBARI_INSTALL_COMPONENTS_FAILED_EVENT)
                    .from(AMBARI_START_COMPONENTS_STATE).to(AMBARI_RESTART_ALL_STATE).event(AMBARI_START_COMPONENTS_FINISHED_EVENT)
                        .failureEvent(AMBARI_START_COMPONENTS_FAILED_EVENT)
                    .from(AMBARI_RESTART_ALL_STATE).to(AMBARI_REPAIR_SINGLE_MASTER_FINISHED_STATE).event(AMBARI_RESTART_ALL_FINISHED_EVENT)
                        .failureEvent(AMBARI_RESTART_ALL_FAILED_EVENT)
                    .from(AMBARI_REPAIR_SINGLE_MASTER_FINISHED_STATE).to(EXECUTING_POSTRECIPES_STATE).event(CLUSTER_UPSCALE_FINISHED_EVENT)
                        .noFailureEvent()

                    // Pathway 2: not single master node
                    .from(UPSCALING_AMBARI_FINISHED_STATE).to(EXECUTING_POSTRECIPES_STATE).event(CLUSTER_UPSCALE_FINISHED_EVENT)
                        .failureEvent(CLUSTER_UPSCALE_FAILED_EVENT)

                    .from(EXECUTING_POSTRECIPES_STATE).to(FINALIZE_UPSCALE_STATE).event(EXECUTE_POSTRECIPES_FINISHED_EVENT)
                        .failureEvent(EXECUTE_POSTRECIPES_FAILED_EVENT)
                    .from(FINALIZE_UPSCALE_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ClusterUpscaleState, ClusterUpscaleEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_UPSCALE_FAILED_STATE, FAIL_HANDLED_EVENT);

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
        return values();
    }

    @Override
    public ClusterUpscaleEvent[] getInitEvents() {
        return new ClusterUpscaleEvent[]{CLUSTER_UPSCALE_TRIGGER_EVENT};
    }
}
