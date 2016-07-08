package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;

public enum StackCreationEvent implements FlowEvent {
    START_CREATION_EVENT(FlowTriggers.STACK_PROVISION_TRIGGER_EVENT),
    SETUP_FINISHED_EVENT(SetupResult.selector(SetupResult.class)),
    SETUP_FAILED_EVENT(SetupResult.failureSelector(SetupResult.class)),
    IMAGE_PREPARATION_FINISHED_EVENT(PrepareImageResult.selector(PrepareImageResult.class)),
    IMAGE_PREPARATION_FAILED_EVENT(PrepareImageResult.failureSelector(PrepareImageResult.class)),
    IMAGE_COPY_CHECK_EVENT("IMAGECOPYCHECK"),
    IMAGE_COPY_FINISHED_EVENT("IMAGECOPYFINISHED"),
    IMAGE_COPY_FAILED_EVENT("IMAGECOPYFAILED"),
    LAUNCH_STACK_FINISHED_EVENT(LaunchStackResult.selector(LaunchStackResult.class)),
    LAUNCH_STACK_FAILED_EVENT(LaunchStackResult.failureSelector(LaunchStackResult.class)),
    COLLECT_METADATA_FINISHED_EVENT(CollectMetadataResult.selector(CollectMetadataResult.class)),
    COLLECT_METADATA_FAILED_EVENT(CollectMetadataResult.failureSelector(CollectMetadataResult.class)),
    SSHFINGERPRINTS_EVENT(GetSSHFingerprintsResult.selector(GetSSHFingerprintsResult.class)),
    SSHFINGERPRINTS_FAILED_EVENT(GetSSHFingerprintsResult.failureSelector(GetSSHFingerprintsResult.class)),
    TLS_SETUP_FINISHED_EVENT("TLS_SETUP_FINISHED_EVENT"),
    STACK_CREATION_FAILED_EVENT("STACK_CREATION_FAILED"),
    STACK_CREATION_FINISHED_EVENT("STACK_CREATION_FINISHED"),
    STACKCREATION_FAILURE_HANDLED_EVENT("STACK_CREATION_FAILHANDLED");

    private String stringRepresentation;

    StackCreationEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
