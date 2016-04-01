package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_STACK_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SSHFINGERPRINTS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_STACKANDCLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.COLLECTMETADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGESETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGE_CHECK_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.PROVISIONING_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.SETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.STACK_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.START_PROVISIONING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.TLS_SETUP_STATE;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackCreationFlowConfig extends AbstractFlowConfiguration<StackCreationState, StackCreationEvent> {

    private static final List<Transition<StackCreationState, StackCreationEvent>> TRANSITIONS = Arrays.asList(
            new Transition<>(INIT_STATE, SETUP_STATE, START_CREATION_EVENT),
            new Transition<>(INIT_STATE, SETUP_STATE, START_STACKANDCLUSTER_CREATION_EVENT),
            new Transition<>(SETUP_STATE, IMAGESETUP_STATE, SETUP_FINISHED_EVENT),
            new Transition<>(IMAGESETUP_STATE, IMAGE_CHECK_STATE, IMAGE_PREPARATION_FINISHED_EVENT),
            new Transition<>(IMAGE_CHECK_STATE, IMAGE_CHECK_STATE, IMAGE_COPY_CHECK_EVENT),
            new Transition<>(IMAGE_CHECK_STATE, START_PROVISIONING_STATE, IMAGE_COPY_FINISHED_EVENT),
            new Transition<>(START_PROVISIONING_STATE, PROVISIONING_FINISHED_STATE, LAUNCH_STACK_FINISHED_EVENT),
            new Transition<>(PROVISIONING_FINISHED_STATE, COLLECTMETADATA_STATE, COLLECT_METADATA_FINISHED_EVENT),
            new Transition<>(COLLECTMETADATA_STATE, TLS_SETUP_STATE, SSHFINGERPRINTS_EVENT)
    );
    private static final FlowEdgeConfig<StackCreationState, StackCreationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TLS_SETUP_STATE, STACK_CREATION_FINISHED_EVENT, STACK_CREATION_FAILED_STATE,
                    STACK_CREATION_FAILED_EVENT);

    private static final EnumSet<StackCreationEvent> OWNEVENTS = EnumSet.complementOf(EnumSet.of(StackCreationEvent.START_STACKANDCLUSTER_CREATION_EVENT));

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
    public FlowEvent[] getEvents() {
        return OWNEVENTS.toArray(new FlowEvent[]{});
    }
}
