package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CHECK_HOST_METADATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CHECK_HOST_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_ENSURE_COMPONENTS_STOPPED_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_ENSURE_COMPONENTS_STOPPED_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_INIT_COMPONENTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_INIT_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_INSTALL_COMPONENTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_INSTALL_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_RESTART_ALL_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_RESTART_ALL_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_START_COMPONENTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_START_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_START_SERVER_AGENT_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_START_SERVER_AGENT_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_STOP_COMPONENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_STOP_SERVER_AGENT_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_MANAGER_STOP_SERVER_AGENT_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_REPAIR_SINGLE_MASTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_POSTRECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_POSTRECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.PREFLIGHT_CHECK_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.RECONFIGURE_KEYTABS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.RECONFIGURE_KEYTABS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPLOAD_UPSCALE_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPLOAD_UPSCALE_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPSCALE_CLUSTER_MANAGER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPSCALE_CLUSTER_MANAGER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.values;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CHECK_HOST_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_ENSURE_COMPONENTS_ARE_STOPPED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_INIT_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_INSTALL_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_REPAIR_SINGLE_MASTER_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_RESTART_ALL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_START_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_START_SERVER_AGENT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_STOP_COMPONENTS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_MANAGER_STOP_SERVER_AGENT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_UPSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.EXECUTING_POSTRECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINALIZE_UPSCALE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.RECONFIGURE_KEYTABS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPLOAD_UPSCALE_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPSCALE_PREFLIGHT_CHECK_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPSCALING_CLUSTER_MANAGER_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPSCALING_CLUSTER_MANAGER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterUpscaleFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ClusterUpscaleState, ClusterUpscaleEvent>
        implements RetryableFlowConfiguration<ClusterUpscaleEvent> {
    private static final List<Transition<ClusterUpscaleState, ClusterUpscaleEvent>> TRANSITIONS =
            new Builder<ClusterUpscaleState, ClusterUpscaleEvent>()
                    .defaultFailureEvent(FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(UPLOAD_UPSCALE_RECIPES_STATE)
                    .event(CLUSTER_UPSCALE_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(UPLOAD_UPSCALE_RECIPES_STATE)
                    .to(UPSCALE_PREFLIGHT_CHECK_STATE)
                    .event(UPLOAD_UPSCALE_RECIPES_FINISHED_EVENT)
                    .failureEvent(UPLOAD_UPSCALE_RECIPES_FAILED_EVENT)

                    .from(UPSCALE_PREFLIGHT_CHECK_STATE)
                    .to(RECONFIGURE_KEYTABS_STATE)
                    .event(PREFLIGHT_CHECK_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(RECONFIGURE_KEYTABS_STATE)
                    .to(CHECK_HOST_METADATA_STATE)
                    .event(RECONFIGURE_KEYTABS_FINISHED_EVENT)
                    .failureEvent(RECONFIGURE_KEYTABS_FAILED_EVENT)

                    .from(CHECK_HOST_METADATA_STATE)
                    .to(UPSCALING_CLUSTER_MANAGER_STATE)
                    .event(CHECK_HOST_METADATA_FINISHED_EVENT)
                    .failureEvent(CHECK_HOST_METADATA_FAILED_EVENT)

                    .from(UPSCALING_CLUSTER_MANAGER_STATE)
                    .to(UPSCALING_CLUSTER_MANAGER_FINISHED_STATE)
                    .event(UPSCALE_CLUSTER_MANAGER_FINISHED_EVENT)
                    .failureEvent(UPSCALE_CLUSTER_MANAGER_FAILED_EVENT)

                    // Pathway 1: repair single master node
                    .from(UPSCALING_CLUSTER_MANAGER_FINISHED_STATE)
                    .to(CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_STATE)
                    .event(CLUSTER_REPAIR_SINGLE_MASTER_START_EVENT)
                    .noFailureEvent()

                    .from(CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_STATE)
                    .to(CLUSTER_MANAGER_STOP_COMPONENTS_STATE)
                    .event(CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_STOP_COMPONENTS_STATE)
                    .to(CLUSTER_MANAGER_STOP_SERVER_AGENT_STATE)
                    .event(CLUSTER_MANAGER_STOP_COMPONENTS_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_STOP_SERVER_AGENT_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_STOP_SERVER_AGENT_STATE)
                    .to(CLUSTER_MANAGER_START_SERVER_AGENT_STATE)
                    .event(CLUSTER_MANAGER_STOP_SERVER_AGENT_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_STOP_SERVER_AGENT_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_START_SERVER_AGENT_STATE)
                    .to(CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_STATE)
                    .event(CLUSTER_MANAGER_START_SERVER_AGENT_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_START_SERVER_AGENT_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_STATE)
                    .to(CLUSTER_MANAGER_ENSURE_COMPONENTS_ARE_STOPPED_STATE)
                    .event(CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_ENSURE_COMPONENTS_ARE_STOPPED_STATE)
                    .to(CLUSTER_MANAGER_INIT_COMPONENTS_STATE)
                    .event(CLUSTER_MANAGER_ENSURE_COMPONENTS_STOPPED_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_ENSURE_COMPONENTS_STOPPED_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_INIT_COMPONENTS_STATE)
                    .to(CLUSTER_MANAGER_INSTALL_COMPONENTS_STATE)
                    .event(CLUSTER_MANAGER_INIT_COMPONENTS_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_INIT_COMPONENTS_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_INSTALL_COMPONENTS_STATE)
                    .to(CLUSTER_MANAGER_START_COMPONENTS_STATE)
                    .event(CLUSTER_MANAGER_INSTALL_COMPONENTS_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_INSTALL_COMPONENTS_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_START_COMPONENTS_STATE)
                    .to(CLUSTER_MANAGER_RESTART_ALL_STATE)
                    .event(CLUSTER_MANAGER_START_COMPONENTS_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_START_COMPONENTS_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_RESTART_ALL_STATE)
                    .to(CLUSTER_MANAGER_REPAIR_SINGLE_MASTER_FINISHED_STATE)
                    .event(CLUSTER_MANAGER_RESTART_ALL_FINISHED_EVENT)
                    .failureEvent(CLUSTER_MANAGER_RESTART_ALL_FAILED_EVENT)

                    .from(CLUSTER_MANAGER_REPAIR_SINGLE_MASTER_FINISHED_STATE)
                    .to(EXECUTING_POSTRECIPES_STATE)
                    .event(CLUSTER_UPSCALE_FINISHED_EVENT)
                    .noFailureEvent()

                    // Pathway 2: not single master node
                    .from(UPSCALING_CLUSTER_MANAGER_FINISHED_STATE)
                    .to(EXECUTING_POSTRECIPES_STATE)
                    .event(CLUSTER_UPSCALE_FINISHED_EVENT)
                    .failureEvent(CLUSTER_UPSCALE_FAILED_EVENT)

                    .from(EXECUTING_POSTRECIPES_STATE)
                    .to(FINALIZE_UPSCALE_STATE)
                    .event(EXECUTE_POSTRECIPES_FINISHED_EVENT)
                    .failureEvent(EXECUTE_POSTRECIPES_FAILED_EVENT)

                    .from(FINALIZE_UPSCALE_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .defaultFailureEvent()

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
    public FlowEdgeConfig<ClusterUpscaleState, ClusterUpscaleEvent> getEdgeConfig() {
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

    @Override
    public String getDisplayName() {
        return "Upscale cluster";
    }

    @Override
    public ClusterUpscaleEvent getRetryableEvent() {
        return FAIL_HANDLED_EVENT;
    }
}
