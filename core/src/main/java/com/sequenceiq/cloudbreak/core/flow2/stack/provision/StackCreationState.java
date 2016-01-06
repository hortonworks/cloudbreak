package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.CheckImageAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.CollectMetadataAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.PrepareImageAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.ProvisioningFinishedAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.ProvisioningSetupAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.StackCreationFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.StartProvisioningAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.TlsSetupAction;

public enum StackCreationState implements FlowState<StackCreationState, StackCreationEvent> {
    INIT_STATE(),
    STACK_CREATION_FAILED_STATE(StackCreationFailureAction.class),
    SETUP_STATE(ProvisioningSetupAction.class, StackCreationEvent.SETUP_FAILED_EVENT),
    IMAGESETUP_STATE(PrepareImageAction.class, StackCreationEvent.IMAGE_PREPARATION_FAILED_EVENT),
    IMAGE_CHECK_STATE(CheckImageAction.class, StackCreationEvent.IMAGE_COPY_FAILED_EVENT),
    START_PROVISIONING_STATE(StartProvisioningAction.class, StackCreationEvent.LAUNCH_STACK_FAILED_EVENT),
    PROVISIONING_FINISHED_STATE(ProvisioningFinishedAction.class, StackCreationEvent.LAUNCH_STACK_FAILED_EVENT),
    COLLECTMETADATA_STATE(CollectMetadataAction.class, StackCreationEvent.COLLECT_METADATA_FAILED_EVENT),
    TLS_SETUP_STATE(TlsSetupAction.class, StackCreationEvent.SSHFINGERPRINTS_FAILED_EVENT),
    FINAL_STATE();

    private Class<?> action;
    private StackCreationEvent failureEvent;
    private StackCreationState failureState;

    StackCreationState() {
    }

    StackCreationState(Class<?> action) {
        this.action = action;
    }

    StackCreationState(Class<?> action, StackCreationEvent failureEvent) {
        this.action = action;
        this.failureEvent = failureEvent;
    }

    StackCreationState(Class<?> action, StackCreationEvent failureEvent, StackCreationState failureState) {
        this.action = action;
        this.failureEvent = failureEvent;
        this.failureState = failureState;
    }

    @Override
    public Class<?> action() {
        return action;
    }

    @Override
    public StackCreationEvent failureEvent() {
        return failureEvent;
    }

    @Override
    public StackCreationState failureState() {
        return failureState;
    }
}
