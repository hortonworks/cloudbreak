package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.BOOTSTRAP_NEW_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.BOOTSTRAP_NEW_NODES_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.CLEANUP_FREEIPA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.CLEANUP_FREEIPA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.CLUSTER_PROXY_RE_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_HOST_METADATA_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_HOST_METADATA_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_HOST_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_HOST_METADATA_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.SSHFINGERPRINTS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.TLS_SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.TLS_SETUP_FINISHED_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPDATE_DOMAIN_DNS_RESOLVER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPDATE_DOMAIN_DNS_RESOLVER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_CREATE_USERDATA_SECRETS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_CREATE_USERDATA_SECRETS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_IMAGE_FALLBACK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_IMAGE_FALLBACK_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_IMAGE_FALLBACK_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_INVALID_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_SALT_INVALID_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_SALT_VALID_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_UPDATE_LOAD_BALANCERS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_UPDATE_LOAD_BALANCERS_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_UPDATE_USERDATA_SECRETS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_UPDATE_USERDATA_SECRETS_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_FAILURE_EVENT;
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
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPDATE_DOMAIN_DNS_RESOLVER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_CREATE_USERDATA_SECRETS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_IMAGE_FALLBACK_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_PREVALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_SALT_PREVALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_UPDATE_LOAD_BALANCERS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_UPDATE_USERDATA_SECRETS_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class StackUpscaleConfig extends StackStatusFinalizerAbstractFlowConfig<StackUpscaleState, StackUpscaleEvent>
        implements RetryableFlowConfiguration<StackUpscaleEvent> {
    private static final List<Transition<StackUpscaleState, StackUpscaleEvent>> TRANSITIONS =
            new Builder<StackUpscaleState, StackUpscaleEvent>()
                    .from(INIT_STATE).to(UPDATE_DOMAIN_DNS_RESOLVER_STATE).event(ADD_INSTANCES_EVENT).noFailureEvent()
                    .from(UPDATE_DOMAIN_DNS_RESOLVER_STATE).to(UPSCALE_SALT_PREVALIDATION_STATE).event(UPDATE_DOMAIN_DNS_RESOLVER_FINISHED_EVENT)
                    .failureEvent(UPDATE_DOMAIN_DNS_RESOLVER_FAILED_EVENT)
                    .from(UPSCALE_SALT_PREVALIDATION_STATE).to(UPSCALE_PREVALIDATION_STATE).event(UPSCALE_SALT_VALID_EVENT)
                    .failureEvent(UPSCALE_SALT_INVALID_EVENT)
                    .from(UPSCALE_PREVALIDATION_STATE).to(UPSCALE_CREATE_USERDATA_SECRETS_STATE).event(UPSCALE_VALID_EVENT).failureEvent(UPSCALE_INVALID_EVENT)
                    .from(UPSCALE_PREVALIDATION_STATE).to(EXTEND_METADATA_STATE).event(EXTEND_METADATA_EVENT).failureEvent(UPSCALE_INVALID_EVENT)

                    .from(UPSCALE_CREATE_USERDATA_SECRETS_STATE).to(ADD_INSTANCES_STATE).event(UPSCALE_CREATE_USERDATA_SECRETS_FINISHED_EVENT)
                    .failureEvent(UPSCALE_CREATE_USERDATA_SECRETS_FAILED_EVENT)
                    .from(ADD_INSTANCES_STATE).to(ADD_INSTANCES_FINISHED_STATE).event(ADD_INSTANCES_FINISHED_EVENT).failureEvent(ADD_INSTANCES_FAILURE_EVENT)
                    .from(ADD_INSTANCES_STATE).to(UPSCALE_IMAGE_FALLBACK_STATE).event(UPSCALE_IMAGE_FALLBACK_EVENT).failureEvent(ADD_INSTANCES_FAILURE_EVENT)
                    .from(UPSCALE_IMAGE_FALLBACK_STATE).to(ADD_INSTANCES_STATE).event(UPSCALE_IMAGE_FALLBACK_FINISHED_EVENT)
                    .failureEvent(UPSCALE_IMAGE_FALLBACK_FAILED_EVENT)

                    .from(ADD_INSTANCES_FINISHED_STATE).to(EXTEND_METADATA_STATE).event(EXTEND_METADATA_EVENT)
                    .failureEvent(ADD_INSTANCES_FINISHED_FAILURE_EVENT)
                    .from(EXTEND_METADATA_STATE).to(EXTEND_METADATA_FINISHED_STATE).event(EXTEND_METADATA_FINISHED_EVENT)
                    .failureEvent(EXTEND_METADATA_FAILURE_EVENT)
                    .from(EXTEND_METADATA_FINISHED_STATE).to(UPSCALE_UPDATE_LOAD_BALANCERS_STATE).event(UPSCALE_UPDATE_LOAD_BALANCERS_EVENT)
                    .failureEvent(EXTEND_METADATA_FINISHED_FAILURE_EVENT)
                    .from(UPSCALE_UPDATE_LOAD_BALANCERS_STATE).to(UPSCALE_UPDATE_USERDATA_SECRETS_STATE).event(UPSCALE_UPDATE_USERDATA_SECRETS_EVENT)
                    .failureEvent(UPSCALE_UPDATE_LOAD_BALANCERS_FAILURE_EVENT)
                    .from(UPSCALE_UPDATE_USERDATA_SECRETS_STATE).to(UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_STATE)
                    .event(UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_EVENT).failureEvent(UPSCALE_UPDATE_USERDATA_SECRETS_FAILURE_EVENT)
                    .from(UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_STATE).to(RE_REGISTER_WITH_CLUSTER_PROXY_STATE).event(BOOTSTRAP_NEW_NODES_EVENT)
                    .failureEvent(UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_FAILURE_EVENT)
                    .from(UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_STATE).to(GATEWAY_TLS_SETUP_STATE).event(SSHFINGERPRINTS_EVENT)
                    .failureEvent(UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_FAILURE_EVENT)

                    .from(GATEWAY_TLS_SETUP_STATE).to(RE_REGISTER_WITH_CLUSTER_PROXY_STATE).event(TLS_SETUP_FINISHED_EVENT)
                    .failureEvent(TLS_SETUP_FINISHED_FAILED_EVENT)

                    .from(RE_REGISTER_WITH_CLUSTER_PROXY_STATE).to(BOOTSTRAP_NEW_NODES_STATE).event(CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT)
                    .failureEvent(CLUSTER_PROXY_RE_REGISTRATION_FAILED_EVENT)
                    .from(BOOTSTRAP_NEW_NODES_STATE).to(EXTEND_HOST_METADATA_STATE).event(EXTEND_HOST_METADATA_EVENT)
                    .failureEvent(BOOTSTRAP_NEW_NODES_FAILURE_EVENT)
                    .from(EXTEND_HOST_METADATA_STATE).to(CLEANUP_FREEIPA_UPSCALE_STATE).event(EXTEND_HOST_METADATA_FINISHED_EVENT)
                    .failureEvent(EXTEND_HOST_METADATA_FAILURE_EVENT)
                    .from(CLEANUP_FREEIPA_UPSCALE_STATE).to(EXTEND_HOST_METADATA_FINISHED_STATE).event(CLEANUP_FREEIPA_FINISHED_EVENT)
                    .failureEvent(CLEANUP_FREEIPA_FAILED_EVENT)
                    .from(EXTEND_HOST_METADATA_FINISHED_STATE).to(FINAL_STATE).event(UPSCALE_FINALIZED_EVENT)
                    .failureEvent(EXTEND_HOST_METADATA_FINISHED_FAILURE_EVENT)
                    .build();

    private static final FlowEdgeConfig<StackUpscaleState, StackUpscaleEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPSCALE_FAILED_STATE, UPSCALE_FAIL_HANDLED_EVENT);

    public StackUpscaleConfig() {
        super(StackUpscaleState.class, StackUpscaleEvent.class);
    }

    @Override
    protected List<Transition<StackUpscaleState, StackUpscaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<StackUpscaleState, StackUpscaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackUpscaleEvent[] getEvents() {
        return StackUpscaleEvent.values();
    }

    @Override
    public StackUpscaleEvent[] getInitEvents() {
        return new StackUpscaleEvent[]{
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
}
