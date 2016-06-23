package com.sequenceiq.cloudbreak.core.flow2.stack.provision

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesFailed
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupFailed
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess

enum class StackCreationEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    START_CREATION_EVENT(FlowTriggers.STACK_PROVISION_TRIGGER_EVENT),
    SETUP_FINISHED_EVENT(SetupResult.selector(SetupResult::class.java)),
    SETUP_FAILED_EVENT(SetupResult.failureSelector(SetupResult::class.java)),
    IMAGE_PREPARATION_FINISHED_EVENT(PrepareImageResult.selector(PrepareImageResult::class.java)),
    IMAGE_PREPARATION_FAILED_EVENT(PrepareImageResult.failureSelector(PrepareImageResult::class.java)),
    IMAGE_COPY_CHECK_EVENT("IMAGECOPYCHECK"),
    IMAGE_COPY_FINISHED_EVENT("IMAGECOPYFINISHED"),
    IMAGE_COPY_FAILED_EVENT("IMAGECOPYFAILED"),
    LAUNCH_STACK_FINISHED_EVENT(LaunchStackResult.selector(LaunchStackResult::class.java)),
    LAUNCH_STACK_FAILED_EVENT(LaunchStackResult.failureSelector(LaunchStackResult::class.java)),
    COLLECT_METADATA_FINISHED_EVENT(CollectMetadataResult.selector(CollectMetadataResult::class.java)),
    COLLECT_METADATA_FAILED_EVENT(CollectMetadataResult.failureSelector(CollectMetadataResult::class.java)),
    SSHFINGERPRINTS_EVENT(GetSSHFingerprintsResult.selector(GetSSHFingerprintsResult::class.java)),
    SSHFINGERPRINTS_FAILED_EVENT(GetSSHFingerprintsResult.failureSelector(GetSSHFingerprintsResult::class.java)),
    BOOTSTRAP_MACHINES_EVENT("BOOTSTRAP_MACHINES_EVENT"),
    BOOTSTRAP_MACHINES_FINISHED_EVENT(EventSelectorUtil.selector(BootstrapMachinesSuccess::class.java)),
    BOOTSTRAP_MACHINES_FAILED_EVENT(EventSelectorUtil.selector(BootstrapMachinesFailed::class.java)),
    HOST_METADATASETUP_FINISHED_EVENT(EventSelectorUtil.selector(HostMetadataSetupSuccess::class.java)),
    HOST_METADATASETUP_FAILED_EVENT(EventSelectorUtil.selector(HostMetadataSetupFailed::class.java)),
    STACK_CREATION_FAILED_EVENT("STACK_CREATION_FAILED"),
    STACK_CREATION_FINISHED_EVENT("STACK_CREATION_FINISHED"),
    STACKCREATION_FAILURE_HANDLED_EVENT("STACK_CREATION_FAILHANDLED");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
