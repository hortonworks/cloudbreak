package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.COLLECT_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.IMAGE_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_STACK_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SETUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.SSHFINGERPRINTS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FAILE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.STACK_CREATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_STACKANDCLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGESETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGE_CHECK_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.PROVISIONING_FINISHED_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.PROVISIONING_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.SAVE_COLLECTEDMETADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.SETUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.STACK_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.START_PROVISIONING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.TLS_SETUP_STATE;

import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackCreationFlowConfig extends AbstractFlowConfiguration<StackCreationState, StackCreationEvent> {

    private static final List<Transition<StackCreationState, StackCreationEvent>> TRANSITIONS = new Transition.Builder<StackCreationState, StackCreationEvent>()
            .from(INIT_STATE).to(SETUP_STATE).event(START_CREATION_EVENT).failure(SETUP_FAILED_EVENT)
            .from(INIT_STATE).to(SETUP_STATE).event(START_STACKANDCLUSTER_CREATION_EVENT).failure(SETUP_FAILED_EVENT)
            .from(SETUP_STATE).to(IMAGESETUP_STATE).event(SETUP_FINISHED_EVENT).failure(StackCreationEvent.IMAGE_PREPARATION_FAILED_EVENT)
            .from(IMAGESETUP_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_PREPARATION_FINISHED_EVENT).failure(StackCreationEvent.IMAGE_COPY_FAILED_EVENT)
            .from(IMAGE_CHECK_STATE).to(IMAGE_CHECK_STATE).event(IMAGE_COPY_CHECK_EVENT).failure(StackCreationEvent.IMAGE_COPY_FAILED_EVENT)
            .from(IMAGE_CHECK_STATE).to(START_PROVISIONING_STATE).event(IMAGE_COPY_FINISHED_EVENT).failure(StackCreationEvent.LAUNCH_STACK_FAILED_EVENT)
            .from(START_PROVISIONING_STATE).to(PROVISIONING_FINISHED_STATE)
                    .event(LAUNCH_STACK_FINISHED_EVENT).failure(StackCreationEvent.COLLECT_METADATA_FAILED_EVENT)
            .from(PROVISIONING_FINISHED_FAILED_STATE).to(PROVISIONING_FINISHED_STATE).event(LAUNCH_STACK_FINISHED_EVENT).defaultFailure()
            .from(PROVISIONING_FINISHED_STATE).to(SAVE_COLLECTEDMETADATA_STATE)
                    .event(COLLECT_METADATA_FINISHED_EVENT).failure(StackCreationEvent.SSHFINGERPRINTS_FAILED_EVENT)
            .from(SAVE_COLLECTEDMETADATA_STATE).to(TLS_SETUP_STATE).event(SSHFINGERPRINTS_EVENT).failure(StackCreationEvent.SSHFINGERPRINTS_FAILED_EVENT)
            .build();

    private static final FlowEdgeConfig<StackCreationState, StackCreationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TLS_SETUP_STATE, STACK_CREATION_FINISHED_EVENT, STACK_CREATION_FAILED_STATE,
                    STACK_CREATION_FAILE_HANDLED_EVENT);

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
    public StackCreationEvent[] getEvents() {
        return OWNEVENTS.toArray(new StackCreationEvent[]{});
    }
}
