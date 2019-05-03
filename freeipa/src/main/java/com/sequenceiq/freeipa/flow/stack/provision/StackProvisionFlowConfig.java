package com.sequenceiq.freeipa.flow.stack.provision;

import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.COLLECT_METADATA_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.COLLECT_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.CREATE_CREDENTIAL_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.CREATE_CREDENTIAL_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.GET_TLS_INFO_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.GET_TLS_INFO_FINISHED_EVENT;
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
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.VALIDATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.COLLECTMETADATA_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.CREATE_CREDENTIAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.GET_TLS_INFO_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.PROVISIONING_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.SETUP_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.STACK_CREATION_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.STACK_CREATION_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.START_PROVISIONING_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.TLS_SETUP_STATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState.VALIDATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.cloudbreak.core.flow2.config.RetryableFlowConfiguration;

@Component
public class StackProvisionFlowConfig extends AbstractFlowConfiguration<StackProvisionState, StackProvisionEvent>
        implements RetryableFlowConfiguration<StackProvisionEvent> {
    private static final List<Transition<StackProvisionState, StackProvisionEvent>> TRANSITIONS = new Builder<StackProvisionState, StackProvisionEvent>()
            .defaultFailureEvent(STACK_CREATION_FAILED_EVENT)
            .from(INIT_STATE).to(VALIDATION_STATE).event(START_CREATION_EVENT).noFailureEvent()
            .from(VALIDATION_STATE).to(SETUP_STATE).event(VALIDATION_FINISHED_EVENT).failureEvent(VALIDATION_FAILED_EVENT)
            .from(SETUP_STATE).to(CREATE_CREDENTIAL_STATE).event(SETUP_FINISHED_EVENT).failureEvent(SETUP_FAILED_EVENT)
            .from(CREATE_CREDENTIAL_STATE).to(START_PROVISIONING_STATE).event(CREATE_CREDENTIAL_FINISHED_EVENT).failureEvent(CREATE_CREDENTIAL_FAILED_EVENT)
            .from(START_PROVISIONING_STATE).to(PROVISIONING_FINISHED_STATE).event(LAUNCH_STACK_FINISHED_EVENT).failureEvent(LAUNCH_STACK_FAILED_EVENT)
            .from(PROVISIONING_FINISHED_STATE).to(COLLECTMETADATA_STATE).event(COLLECT_METADATA_FINISHED_EVENT).failureEvent(COLLECT_METADATA_FAILED_EVENT)
            .from(COLLECTMETADATA_STATE).to(GET_TLS_INFO_STATE).event(GET_TLS_INFO_FINISHED_EVENT).failureEvent(GET_TLS_INFO_FAILED_EVENT)
            .from(GET_TLS_INFO_STATE).to(TLS_SETUP_STATE).event(SETUP_TLS_EVENT).defaultFailureEvent()
            .from(TLS_SETUP_STATE).to(STACK_CREATION_FINISHED_STATE).event(TLS_SETUP_FINISHED_EVENT).defaultFailureEvent()
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
    protected FlowEdgeConfig<StackProvisionState, StackProvisionEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackProvisionEvent[] getEvents() {
        return StackProvisionEvent.values();
    }

    @Override
    public StackProvisionEvent[] getInitEvents() {
        return new StackProvisionEvent[] {
                START_CREATION_EVENT
        };
    }

    @Override
    public StackProvisionEvent getFailHandledEvent() {
        return STACKCREATION_FAILURE_HANDLED_EVENT;
    }
}
