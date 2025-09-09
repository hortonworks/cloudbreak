package com.sequenceiq.freeipa.flow.stack.provision;

import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.CLUSTER_PROXY_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.COLLECT_METADATA_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.COLLECT_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.CREATE_CREDENTIAL_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.CREATE_CREDENTIAL_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.CREATE_USER_DATA_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.CREATE_USER_DATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.GENERATE_ENCRYPTION_KEYS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.GENERATE_ENCRYPTION_KEYS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.GET_TLS_INFO_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.GET_TLS_INFO_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_COPY_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_FALLBACK_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_FALLBACK_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_FALLBACK_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_FALLBACK_START_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_PREPARATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.LAUNCH_STACK_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.LAUNCH_STACK_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.SETUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.SETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.SETUP_TLS_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.STACKCREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.STACK_CREATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.STACK_CREATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.START_CREATION_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.TLS_SETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.UPDATE_USERDATA_SECRETS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.UPDATE_USERDATA_SECRETS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.VALIDATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.CLUSTERPROXY_REGISTRATION_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.COLLECTMETADATA_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.CREATE_CREDENTIAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.CREATE_USER_DATA_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.GENERATE_ENCRYPTION_KEYS_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.GET_TLS_INFO_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.IMAGESETUP_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.IMAGE_CHECK_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.IMAGE_FALLBACK_START_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.IMAGE_FALLBACK_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.PROVISIONING_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.SETUP_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.STACK_CREATION_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.STACK_CREATION_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.START_PROVISIONING_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.TLS_SETUP_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.UPDATE_USERDATA_SECRETS_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.VALIDATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class StackProvisionFlowConfig extends StackStatusFinalizerAbstractFlowConfig<StackProvisionState, StackProvisionEvent>
        implements RetryableFlowConfiguration<StackProvisionEvent> {
    private static final List<Transition<StackProvisionState, StackProvisionEvent>> TRANSITIONS = new Builder<StackProvisionState, StackProvisionEvent>()
            .defaultFailureEvent(STACK_CREATION_FAILED_EVENT)
            .from(INIT_STATE).to(VALIDATION_STATE).event(START_CREATION_EVENT).noFailureEvent()
            .from(VALIDATION_STATE).to(GENERATE_ENCRYPTION_KEYS_STATE).event(VALIDATION_FINISHED_EVENT).failureEvent(VALIDATION_FAILED_EVENT)
            .from(GENERATE_ENCRYPTION_KEYS_STATE).to(CREATE_USER_DATA_STATE).event(GENERATE_ENCRYPTION_KEYS_FINISHED_EVENT)
            .failureEvent(GENERATE_ENCRYPTION_KEYS_FAILED_EVENT)
            .from(CREATE_USER_DATA_STATE).to(SETUP_STATE).event(CREATE_USER_DATA_FINISHED_EVENT).failureEvent(CREATE_USER_DATA_FAILED_EVENT)
            .from(SETUP_STATE).to(IMAGESETUP_STATE).event(SETUP_FINISHED_EVENT).failureEvent(SETUP_FAILED_EVENT)
            .from(IMAGESETUP_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_PREPARATION_FINISHED_EVENT).failureEvent(IMAGE_PREPARATION_FAILED_EVENT)
            .from(IMAGE_CHECK_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_COPY_CHECK_EVENT).failureEvent(IMAGE_COPY_FAILED_EVENT)
            .from(IMAGE_CHECK_STATE).to(CREATE_CREDENTIAL_STATE).event(IMAGE_COPY_FINISHED_EVENT).failureEvent(IMAGE_COPY_FAILED_EVENT)
            .from(CREATE_CREDENTIAL_STATE).to(START_PROVISIONING_STATE).event(CREATE_CREDENTIAL_FINISHED_EVENT).failureEvent(CREATE_CREDENTIAL_FAILED_EVENT)
            .from(START_PROVISIONING_STATE).to(PROVISIONING_FINISHED_STATE).event(LAUNCH_STACK_FINISHED_EVENT).failureEvent(LAUNCH_STACK_FAILED_EVENT)
            .from(START_PROVISIONING_STATE).to(IMAGE_FALLBACK_STATE).event(IMAGE_FALLBACK_EVENT).failureEvent(LAUNCH_STACK_FAILED_EVENT)
            .from(IMAGE_FALLBACK_STATE).to(IMAGE_FALLBACK_START_STATE).event(IMAGE_FALLBACK_START_EVENT).failureEvent(IMAGE_FALLBACK_FAILED_EVENT)
            .from(IMAGE_FALLBACK_START_STATE).to(IMAGESETUP_STATE).event(IMAGE_FALLBACK_FINISHED_EVENT).failureEvent(IMAGE_FALLBACK_FAILED_EVENT)
            .from(PROVISIONING_FINISHED_STATE).to(COLLECTMETADATA_STATE).event(COLLECT_METADATA_FINISHED_EVENT).failureEvent(COLLECT_METADATA_FAILED_EVENT)
            .from(COLLECTMETADATA_STATE).to(UPDATE_USERDATA_SECRETS_STATE)
            .event(UPDATE_USERDATA_SECRETS_FINISHED_EVENT).failureEvent(UPDATE_USERDATA_SECRETS_FAILED_EVENT)
            .from(UPDATE_USERDATA_SECRETS_STATE).to(GET_TLS_INFO_STATE).event(GET_TLS_INFO_FINISHED_EVENT).failureEvent(GET_TLS_INFO_FAILED_EVENT)
            .from(GET_TLS_INFO_STATE).to(TLS_SETUP_STATE).event(SETUP_TLS_EVENT).defaultFailureEvent()
            .from(TLS_SETUP_STATE).to(CLUSTERPROXY_REGISTRATION_STATE).event(TLS_SETUP_FINISHED_EVENT).defaultFailureEvent()
            .from(CLUSTERPROXY_REGISTRATION_STATE).to(STACK_CREATION_FINISHED_STATE)
            .event(CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT).failureEvent(CLUSTER_PROXY_REGISTRATION_FAILED_EVENT)
            .from(STACK_CREATION_FINISHED_STATE).to(FINAL_STATE).event(STACK_CREATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StackProvisionState, StackProvisionEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STACK_CREATION_FAILED_STATE, STACKCREATION_FAILURE_HANDLED_EVENT);

    public StackProvisionFlowConfig() {
        super(StackProvisionState.class, StackProvisionEvent.class);
    }

    @Override
    protected List<Transition<StackProvisionState, StackProvisionEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<StackProvisionState, StackProvisionEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackProvisionEvent[] getEvents() {
        return StackProvisionEvent.values();
    }

    @Override
    public StackProvisionEvent[] getInitEvents() {
        return new StackProvisionEvent[]{
                START_CREATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Provision stack";
    }

    @Override
    public StackProvisionEvent getRetryableEvent() {
        return STACKCREATION_FAILURE_HANDLED_EVENT;
    }
}
