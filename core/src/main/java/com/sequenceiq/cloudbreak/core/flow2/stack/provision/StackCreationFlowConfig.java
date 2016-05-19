package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.BOOTSTRAP_MACHINES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_METADATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.HOST_METADATASETUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.HOST_METADATASETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_PREPARATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_PREPARATION_FINISHED_EVENT;
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
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_STACKANDCLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.BOOTSTRAPING_MACHINES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.COLLECTING_HOST_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.COLLECTMETADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGESETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGE_CHECK_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.PROVISIONING_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.SETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.STACK_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.STACK_CREATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.START_PROVISIONING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.TLS_SETUP_STATE;

import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackCreationFlowConfig extends AbstractFlowConfiguration<StackCreationState, StackCreationEvent> {
    private static final List<Transition<StackCreationState, StackCreationEvent>> TRANSITIONS = new Transition.Builder<StackCreationState, StackCreationEvent>()
            .defaultFailureEvent(STACK_CREATION_FAILED_EVENT)
            .from(INIT_STATE).to(SETUP_STATE).event(START_CREATION_EVENT).noFailureEvent()
            .from(INIT_STATE).to(SETUP_STATE).event(START_STACKANDCLUSTER_CREATION_EVENT).noFailureEvent()
            .from(SETUP_STATE).to(IMAGESETUP_STATE).event(SETUP_FINISHED_EVENT).failureEvent(SETUP_FAILED_EVENT)
            .from(IMAGESETUP_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_PREPARATION_FINISHED_EVENT).failureEvent(IMAGE_PREPARATION_FAILED_EVENT)
            .from(IMAGE_CHECK_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_COPY_CHECK_EVENT).failureEvent(IMAGE_COPY_FAILED_EVENT)
            .from(IMAGE_CHECK_STATE).to(START_PROVISIONING_STATE).event(IMAGE_COPY_FINISHED_EVENT).failureEvent(IMAGE_COPY_FAILED_EVENT)
            .from(START_PROVISIONING_STATE).to(PROVISIONING_FINISHED_STATE).event(LAUNCH_STACK_FINISHED_EVENT).failureEvent(LAUNCH_STACK_FAILED_EVENT)
            .from(PROVISIONING_FINISHED_STATE).to(COLLECTMETADATA_STATE).event(COLLECT_METADATA_FINISHED_EVENT).failureEvent(COLLECT_METADATA_FAILED_EVENT)
            .from(COLLECTMETADATA_STATE).to(TLS_SETUP_STATE).event(SSHFINGERPRINTS_EVENT).failureEvent(SSHFINGERPRINTS_FAILED_EVENT)
            .from(TLS_SETUP_STATE).to(BOOTSTRAPING_MACHINES_STATE).event(BOOTSTRAP_MACHINES_EVENT).defaultFailureEvent()
            .from(BOOTSTRAPING_MACHINES_STATE).to(COLLECTING_HOST_METADATA_STATE).event(BOOTSTRAP_MACHINES_FINISHED_EVENT)
                .failureEvent(BOOTSTRAP_MACHINES_FAILED_EVENT)
            .from(COLLECTING_HOST_METADATA_STATE).to(STACK_CREATION_FINISHED_STATE).event(HOST_METADATASETUP_FINISHED_EVENT)
                .failureEvent(HOST_METADATASETUP_FAILED_EVENT)
            .from(STACK_CREATION_FINISHED_STATE).to(FINAL_STATE).event(STACK_CREATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StackCreationState, StackCreationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STACK_CREATION_FAILED_STATE, STACKCREATION_FAILURE_HANDLED_EVENT);

    private static final EnumSet<StackCreationEvent> OWNEVENTS = EnumSet.complementOf(EnumSet.of(START_STACKANDCLUSTER_CREATION_EVENT));

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
        return OWNEVENTS.toArray(new StackCreationEvent[]{});
    }

    @Override
    public StackCreationEvent[] getInitEvents() {
        return new StackCreationEvent[] {
                StackCreationEvent.START_CREATION_EVENT
        };
    }
}
