package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetTlsInfoResult;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchLoadBalancerResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionSchedulingFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionSchedulingSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum StackCreationEvent implements FlowEvent {
    START_CREATION_EVENT("STACK_PROVISION_TRIGGER_EVENT"),
    VALIDATION_FINISHED_EVENT(CloudPlatformResult.selector(ValidationResult.class)),
    VALIDATION_FAILED_EVENT(CloudPlatformResult.failureSelector(ValidationResult.class)),
    GENERATE_ENCRYPTION_KEYS_FINISHED_EVENT(EventSelectorUtil.selector(GenerateEncryptionKeysSuccess.class)),
    GENERATE_ENCRYPTION_KEYS_FAILED_EVENT(EventSelectorUtil.selector(GenerateEncryptionKeysFailed.class)),
    CREATE_USER_DATA_FINISHED_EVENT(EventSelectorUtil.selector(CreateUserDataSuccess.class)),
    CREATE_USER_DATA_FAILED_EVENT(EventSelectorUtil.selector(CreateUserDataFailed.class)),
    SETUP_FINISHED_EVENT(CloudPlatformResult.selector(SetupResult.class)),
    SETUP_FAILED_EVENT(CloudPlatformResult.failureSelector(SetupResult.class)),
    IMAGE_PREPARATION_FINISHED_EVENT(CloudPlatformResult.selector(PrepareImageResult.class)),
    IMAGE_PREPARATION_FAILED_EVENT(CloudPlatformResult.failureSelector(PrepareImageResult.class)),
    IMAGE_COPY_CHECK_EVENT("IMAGECOPYCHECK"),
    IMAGE_COPY_FINISHED_EVENT("IMAGECOPYFINISHED"),
    IMAGE_COPY_FAILED_EVENT("IMAGECOPYFAILED"),
    CREATE_CREDENTIAL_FINISHED_EVENT(CloudPlatformResult.selector(CreateCredentialResult.class)),
    CREATE_CREDENTIAL_FAILED_EVENT(CloudPlatformResult.failureSelector(CreateCredentialResult.class)),
    LAUNCH_STACK_FINISHED_EVENT(CloudPlatformResult.selector(LaunchStackResult.class)),
    LAUNCH_STACK_FAILED_EVENT(CloudPlatformResult.failureSelector(LaunchStackResult.class)),
    IMAGE_FALLBACK_EVENT("IMAGEFALLBACK"),
    IMAGE_FALLBACK_FINISHED_EVENT(EventSelectorUtil.selector(ImageFallbackSuccess.class)),
    IMAGE_FALLBACK_FAILED_EVENT(EventSelectorUtil.selector(ImageFallbackFailed.class)),
    LAUNCH_LOAD_BALANCER_FINISHED_EVENT(CloudPlatformResult.selector(LaunchLoadBalancerResult.class)),
    LAUNCH_LOAD_BALANCER_FAILED_EVENT(CloudPlatformResult.failureSelector(LaunchLoadBalancerResult.class)),
    COLLECT_METADATA_FINISHED_EVENT(CloudPlatformResult.selector(CollectMetadataResult.class)),
    COLLECT_METADATA_FAILED_EVENT(CloudPlatformResult.failureSelector(CollectMetadataResult.class)),
    COLLECT_LOADBALANCER_METADATA_FINISHED_EVENT(CloudPlatformResult.selector(CollectLoadBalancerMetadataResult.class)),
    COLLECT_LOADBALANCER_METADATA_FAILED_EVENT(CloudPlatformResult.failureSelector(CollectLoadBalancerMetadataResult.class)),
    UPDATE_USERDATA_SECRETS_FINISHED_EVENT(EventSelectorUtil.selector(UpdateUserdataSecretsSuccess.class)),
    UPDATE_USERDATA_SECRETS_FAILED_EVENT(EventSelectorUtil.selector(UpdateUserdataSecretsFailed.class)),
    SSHFINGERPRINTS_EVENT(CloudPlatformResult.selector(GetSSHFingerprintsResult.class)),
    SSHFINGERPRINTS_FAILED_EVENT(CloudPlatformResult.failureSelector(GetSSHFingerprintsResult.class)),
    GET_TLS_INFO_FINISHED_EVENT(CloudPlatformResult.selector(GetTlsInfoResult.class)),
    GET_TLS_INFO_FAILED_EVENT(CloudPlatformResult.failureSelector(GetTlsInfoResult.class)),
    TLS_SETUP_FINISHED_EVENT("TLS_SETUP_FINISHED_EVENT"),
    ATTACHED_VOLUME_CONSUMPTION_COLLECTION_SCHEDULING_FINISHED_EVENT(EventSelectorUtil.selector(AttachedVolumeConsumptionCollectionSchedulingSuccess.class)),
    ATTACHED_VOLUME_CONSUMPTION_COLLECTION_SCHEDULING_FAILED_EVENT(EventSelectorUtil.selector(AttachedVolumeConsumptionCollectionSchedulingFailed.class)),
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
