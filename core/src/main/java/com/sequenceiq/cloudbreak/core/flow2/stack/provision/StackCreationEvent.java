package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetTlsInfoResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackCreationEvent implements FlowEvent {
    START_CREATION_EVENT("STACK_PROVISION_TRIGGER_EVENT"),
    VALIDATION_FINISHED_EVENT(ValidationResult.selector(ValidationResult.class)),
    VALIDATION_FAILED_EVENT(ValidationResult.failureSelector(ValidationResult.class)),
    SETUP_FINISHED_EVENT(SetupResult.selector(SetupResult.class)),
    SETUP_FAILED_EVENT(SetupResult.failureSelector(SetupResult.class)),
    IMAGE_PREPARATION_FINISHED_EVENT(PrepareImageResult.selector(PrepareImageResult.class)),
    IMAGE_PREPARATION_FAILED_EVENT(PrepareImageResult.failureSelector(PrepareImageResult.class)),
    IMAGE_COPY_CHECK_EVENT("IMAGECOPYCHECK"),
    IMAGE_COPY_FINISHED_EVENT("IMAGECOPYFINISHED"),
    IMAGE_COPY_FAILED_EVENT("IMAGECOPYFAILED"),
    CREATE_CREDENTIAL_FINISHED_EVENT(CreateCredentialResult.selector(CreateCredentialResult.class)),
    CREATE_CREDENTIAL_FAILED_EVENT(CreateCredentialResult.failureSelector(CreateCredentialResult.class)),
    LAUNCH_STACK_FINISHED_EVENT(LaunchStackResult.selector(LaunchStackResult.class)),
    LAUNCH_STACK_FAILED_EVENT(LaunchStackResult.failureSelector(LaunchStackResult.class)),
    COLLECT_METADATA_FINISHED_EVENT(CollectMetadataResult.selector(CollectMetadataResult.class)),
    COLLECT_METADATA_FAILED_EVENT(CollectMetadataResult.failureSelector(CollectMetadataResult.class)),
    SSHFINGERPRINTS_EVENT(GetSSHFingerprintsResult.selector(GetSSHFingerprintsResult.class)),
    SSHFINGERPRINTS_FAILED_EVENT(GetSSHFingerprintsResult.failureSelector(GetSSHFingerprintsResult.class)),
    GET_TLS_INFO_FINISHED_EVENT(GetTlsInfoResult.selector(GetTlsInfoResult.class)),
    GET_TLS_INFO_FAILED_EVENT(GetTlsInfoResult.failureSelector(GetTlsInfoResult.class)),
    TLS_SETUP_FINISHED_EVENT("TLS_SETUP_FINISHED_EVENT"),
    STACK_CREATION_FAILED_EVENT("STACK_CREATION_FAILED"),
    STACK_CREATION_FINISHED_EVENT("STACK_CREATION_FINISHED"),
    STACKCREATION_FAILURE_HANDLED_EVENT("STACK_CREATION_FAILHANDLED");

    private final String event;

    StackCreationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
