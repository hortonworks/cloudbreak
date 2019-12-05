package com.sequenceiq.freeipa.flow.stack.provision;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetTlsInfoResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationSuccess;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataSuccess;

public enum StackProvisionEvent implements FlowEvent {
    START_CREATION_EVENT("STACK_PROVISION_TRIGGER_EVENT"),
    VALIDATION_FINISHED_EVENT(CloudPlatformResult.selector(ValidationResult.class)),
    VALIDATION_FAILED_EVENT(CloudPlatformResult.failureSelector(ValidationResult.class)),
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
    COLLECT_METADATA_FINISHED_EVENT(CloudPlatformResult.selector(CollectMetadataResult.class)),
    COLLECT_METADATA_FAILED_EVENT(CloudPlatformResult.failureSelector(CollectMetadataResult.class)),
    SSHFINGERPRINTS_EVENT(CloudPlatformResult.selector(GetSSHFingerprintsResult.class)),
    SSHFINGERPRINTS_FAILED_EVENT(CloudPlatformResult.failureSelector(GetSSHFingerprintsResult.class)),
    GET_TLS_INFO_FINISHED_EVENT(CloudPlatformResult.selector(GetTlsInfoResult.class)),
    GET_TLS_INFO_FAILED_EVENT(CloudPlatformResult.failureSelector(GetTlsInfoResult.class)),
    TLS_SETUP_FINISHED_EVENT("TLS_SETUP_FINISHED_EVENT"),
    SETUP_TLS_EVENT("SETUP_TLS_EVENT"),
    CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterProxyRegistrationSuccess.class)),
    CLUSTER_PROXY_REGISTRATION_FAILED_EVENT(EventSelectorUtil.selector(ClusterProxyRegistrationFailed.class)),
    STACK_CREATION_FAILED_EVENT("STACK_CREATION_FAILED"),
    STACK_CREATION_FINISHED_EVENT("STACK_CREATION_FINISHED"),
    STACKCREATION_FAILURE_HANDLED_EVENT("STACK_CREATION_FAILHANDLED");

    private final String event;

    StackProvisionEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
