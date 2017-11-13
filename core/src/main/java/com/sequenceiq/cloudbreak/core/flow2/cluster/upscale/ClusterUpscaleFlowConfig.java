package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

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
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_UPSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.EXECUTING_POSTRECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINALIZE_UPSCALE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPLOAD_UPSCALE_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPSCALING_AMBARI_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPSCALING_CLUSTER_STATE;

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
                    .from(UPLOAD_UPSCALE_RECIPES_STATE).to(UPSCALING_AMBARI_STATE).event(UPLOAD_UPSCALE_RECIPES_FINISHED_EVENT)
                        .failureEvent(UPLOAD_UPSCALE_RECIPES_FAILED_EVENT)
                    .from(UPSCALING_AMBARI_STATE).to(UPSCALING_CLUSTER_STATE).event(UPSCALE_AMBARI_FINISHED_EVENT)
                        .failureEvent(UPSCALE_AMBARI_FAILED_EVENT)
                    .from(UPSCALING_CLUSTER_STATE).to(EXECUTING_POSTRECIPES_STATE).event(CLUSTER_UPSCALE_FINISHED_EVENT)
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
