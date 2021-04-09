package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_LOADBALANCER_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_METADATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.CREATE_CREDENTIAL_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.CREATE_CREDENTIAL_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.CREATE_USER_DATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.CREATE_USER_DATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.GET_TLS_INFO_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.GET_TLS_INFO_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_PREPARATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_LOAD_BALANCER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_LOAD_BALANCER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_STACK_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_STACK_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SETUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SSHFINGERPRINTS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SSHFINGERPRINTS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACKCREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.TLS_SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.VALIDATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.COLLECTMETADATA_LOADBALANCER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.COLLECTMETADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.CREATE_CREDENTIAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.CREATE_USER_DATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.GET_TLS_INFO_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGESETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGE_CHECK_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.PROVISIONING_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.PROVISION_LOAD_BALANCER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.SETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.STACK_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.STACK_CREATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.START_PROVISIONING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.TLS_SETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.VALIDATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class StackCreationFlowConfig extends AbstractFlowConfiguration<StackCreationState, StackCreationEvent>
        implements RetryableFlowConfiguration<StackCreationEvent> {
    private static final List<Transition<StackCreationState, StackCreationEvent>> TRANSITIONS = new Builder<StackCreationState, StackCreationEvent>()
            .defaultFailureEvent(STACK_CREATION_FAILED_EVENT)
            .from(INIT_STATE).to(VALIDATION_STATE).event(START_CREATION_EVENT).noFailureEvent()
            .from(VALIDATION_STATE).to(CREATE_USER_DATA_STATE).event(VALIDATION_FINISHED_EVENT).failureEvent(VALIDATION_FAILED_EVENT)
            .from(CREATE_USER_DATA_STATE).to(SETUP_STATE).event(CREATE_USER_DATA_FINISHED_EVENT).failureEvent(CREATE_USER_DATA_FAILED_EVENT)
            .from(SETUP_STATE).to(IMAGESETUP_STATE).event(SETUP_FINISHED_EVENT).failureEvent(SETUP_FAILED_EVENT)
            .from(IMAGESETUP_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_PREPARATION_FINISHED_EVENT).failureEvent(IMAGE_PREPARATION_FAILED_EVENT)
            .from(IMAGE_CHECK_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_COPY_CHECK_EVENT).failureEvent(IMAGE_COPY_FAILED_EVENT)
            .from(IMAGE_CHECK_STATE).to(CREATE_CREDENTIAL_STATE).event(IMAGE_COPY_FINISHED_EVENT).failureEvent(IMAGE_COPY_FAILED_EVENT)
            .from(CREATE_CREDENTIAL_STATE).to(START_PROVISIONING_STATE).event(CREATE_CREDENTIAL_FINISHED_EVENT).failureEvent(CREATE_CREDENTIAL_FAILED_EVENT)
            .from(START_PROVISIONING_STATE).to(PROVISION_LOAD_BALANCER_STATE).event(LAUNCH_STACK_FINISHED_EVENT).failureEvent(LAUNCH_STACK_FAILED_EVENT)
            .from(PROVISION_LOAD_BALANCER_STATE).to(PROVISIONING_FINISHED_STATE).event(LAUNCH_LOAD_BALANCER_FINISHED_EVENT)
                    .failureEvent(LAUNCH_LOAD_BALANCER_FAILED_EVENT)
            .from(PROVISIONING_FINISHED_STATE).to(COLLECTMETADATA_STATE).event(COLLECT_METADATA_FINISHED_EVENT).failureEvent(COLLECT_METADATA_FAILED_EVENT)
            .from(COLLECTMETADATA_STATE).to(COLLECTMETADATA_LOADBALANCER_STATE).event(COLLECT_LOADBALANCER_METADATA_FINISHED_EVENT)
                    .failureEvent(COLLECT_METADATA_FAILED_EVENT)
            .from(COLLECTMETADATA_LOADBALANCER_STATE).to(GET_TLS_INFO_STATE).event(GET_TLS_INFO_FINISHED_EVENT).failureEvent(GET_TLS_INFO_FAILED_EVENT)
            .from(GET_TLS_INFO_STATE).to(TLS_SETUP_STATE).event(SSHFINGERPRINTS_EVENT).failureEvent(SSHFINGERPRINTS_FAILED_EVENT)
            .from(TLS_SETUP_STATE).to(STACK_CREATION_FINISHED_STATE).event(TLS_SETUP_FINISHED_EVENT).defaultFailureEvent()
            .from(STACK_CREATION_FINISHED_STATE).to(FINAL_STATE).event(STACK_CREATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StackCreationState, StackCreationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STACK_CREATION_FAILED_STATE, STACKCREATION_FAILURE_HANDLED_EVENT);

    public StackCreationFlowConfig() {
        super(StackCreationState.class, StackCreationEvent.class);
    }

    @Override
    protected List<Transition<StackCreationState, StackCreationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackCreationState, StackCreationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackCreationEvent[] getEvents() {
        return StackCreationEvent.values();
    }

    @Override
    public StackCreationEvent[] getInitEvents() {
        return new StackCreationEvent[]{
                START_CREATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Create stack";
    }

    @Override
    public StackCreationEvent getRetryableEvent() {
        return STACKCREATION_FAILURE_HANDLED_EVENT;
    }
}
