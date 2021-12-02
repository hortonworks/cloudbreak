package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.CLEANUP_FREEIPA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.CLEANUP_FREEIPA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.CLUSTER_PROXY_RE_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_INVALID_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_VALID_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.ADD_INSTANCES_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.ADD_INSTANCES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.BOOTSTRAP_NEW_NODES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.CLEANUP_FREEIPA_UPSCALE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_HOST_METADATA_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_HOST_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_METADATA_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.GATEWAY_TLS_SETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.RE_REGISTER_WITH_CLUSTER_PROXY_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_PREVALIDATION_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class StackUpscaleConfig extends AbstractFlowConfiguration<StackUpscaleState, StackUpscaleEvent>
        implements RetryableFlowConfiguration<StackUpscaleEvent> {
    private static final List<Transition<StackUpscaleState, StackUpscaleEvent>> TRANSITIONS =
            new Builder<StackUpscaleState, StackUpscaleEvent>()
                    .from(INIT_STATE).to(UPSCALE_PREVALIDATION_STATE).event(ADD_INSTANCES_EVENT).noFailureEvent()
                    .from(UPSCALE_PREVALIDATION_STATE).to(ADD_INSTANCES_STATE).event(UPSCALE_VALID_EVENT).failureEvent(UPSCALE_INVALID_EVENT)
                    .from(UPSCALE_PREVALIDATION_STATE).to(ADD_INSTANCES_FINISHED_STATE).event(ADD_INSTANCES_FINISHED_EVENT).failureEvent(UPSCALE_INVALID_EVENT)
                    .from(ADD_INSTANCES_STATE).to(ADD_INSTANCES_FINISHED_STATE).event(ADD_INSTANCES_FINISHED_EVENT).failureEvent(ADD_INSTANCES_FAILURE_EVENT)
                    .from(ADD_INSTANCES_FINISHED_STATE).to(EXTEND_METADATA_STATE).event(EXTEND_METADATA_EVENT)
                                    .failureEvent(ADD_INSTANCES_FINISHED_FAILURE_EVENT)
                    .from(EXTEND_METADATA_STATE).to(EXTEND_METADATA_FINISHED_STATE).event(EXTEND_METADATA_FINISHED_EVENT)
                                    .failureEvent(EXTEND_METADATA_FAILURE_EVENT)
                    .from(EXTEND_METADATA_FINISHED_STATE).to(RE_REGISTER_WITH_CLUSTER_PROXY_STATE).event(StackUpscaleEvent.BOOTSTRAP_NEW_NODES_EVENT)
                                    .failureEvent(StackUpscaleEvent.EXTEND_METADATA_FINISHED_FAILURE_EVENT)
                    .from(EXTEND_METADATA_FINISHED_STATE).to(GATEWAY_TLS_SETUP_STATE).event(StackUpscaleEvent.SSHFINGERPRINTS_EVENT)
                                    .failureEvent(StackUpscaleEvent.EXTEND_METADATA_FINISHED_FAILURE_EVENT)
                    .from(GATEWAY_TLS_SETUP_STATE).to(RE_REGISTER_WITH_CLUSTER_PROXY_STATE).event(StackUpscaleEvent.TLS_SETUP_FINISHED_EVENT)
                                    .failureEvent(StackUpscaleEvent.TLS_SETUP_FINISHED_FAILED_EVENT)
                    .from(RE_REGISTER_WITH_CLUSTER_PROXY_STATE).to(BOOTSTRAP_NEW_NODES_STATE).event(CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT)
                    .failureEvent(CLUSTER_PROXY_RE_REGISTRATION_FAILED_EVENT)
                    .from(BOOTSTRAP_NEW_NODES_STATE).to(EXTEND_HOST_METADATA_STATE).event(StackUpscaleEvent.EXTEND_HOST_METADATA_EVENT)
                                    .failureEvent(StackUpscaleEvent.BOOTSTRAP_NEW_NODES_FAILURE_EVENT)
                    .from(EXTEND_HOST_METADATA_STATE).to(CLEANUP_FREEIPA_UPSCALE_STATE).event(StackUpscaleEvent.EXTEND_HOST_METADATA_FINISHED_EVENT)
                                    .failureEvent(StackUpscaleEvent.EXTEND_HOST_METADATA_FAILURE_EVENT)
                    .from(CLEANUP_FREEIPA_UPSCALE_STATE).to(EXTEND_HOST_METADATA_FINISHED_STATE).event(CLEANUP_FREEIPA_FINISHED_EVENT)
                                    .failureEvent(CLEANUP_FREEIPA_FAILED_EVENT)
                    .from(EXTEND_HOST_METADATA_FINISHED_STATE).to(FINAL_STATE).event(UPSCALE_FINALIZED_EVENT)
                                    .failureEvent(StackUpscaleEvent.EXTEND_HOST_METADATA_FINISHED_FAILURE_EVENT)
                    .build();

    private static final FlowEdgeConfig<StackUpscaleState, StackUpscaleEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPSCALE_FAILED_STATE, UPSCALE_FAIL_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public StackUpscaleConfig() {
        super(StackUpscaleState.class, StackUpscaleEvent.class);
    }

    @Override
    protected List<Transition<StackUpscaleState, StackUpscaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackUpscaleState, StackUpscaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackUpscaleEvent[] getEvents() {
        return StackUpscaleEvent.values();
    }

    @Override
    public StackUpscaleEvent[] getInitEvents() {
        return new StackUpscaleEvent[] {
                ADD_INSTANCES_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Upscale stack";
    }

    @Override
    public StackUpscaleEvent getRetryableEvent() {
        return UPSCALE_FAIL_HANDLED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
