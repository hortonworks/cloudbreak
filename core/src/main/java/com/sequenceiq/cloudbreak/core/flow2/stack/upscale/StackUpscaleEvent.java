package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.MountDisksOnNewHostsResult;

public enum StackUpscaleEvent implements FlowEvent {
    ADD_INSTANCES_EVENT("STACK_UPSCALE_TRIGGER_EVENT"),
    UPSCALE_VALID_EVENT(CloudPlatformResult.selector(UpscaleStackValidationResult.class)),
    UPSCALE_INVALID_EVENT(CloudPlatformResult.failureSelector(UpscaleStackValidationResult.class)),
    ADD_INSTANCES_FINISHED_EVENT(CloudPlatformResult.selector(UpscaleStackResult.class)),
    ADD_INSTANCES_FAILURE_EVENT(CloudPlatformResult.failureSelector(UpscaleStackResult.class)),
    ADD_INSTANCES_FINISHED_FAILURE_EVENT("ADD_INSTANCES_FINISHED_FAILURE_EVENT"),
    EXTEND_METADATA_EVENT("EXTEND_METADATA"),
    EXTEND_METADATA_FINISHED_EVENT(CloudPlatformResult.selector(CollectMetadataResult.class)),
    EXTEND_METADATA_FAILURE_EVENT(CloudPlatformResult.failureSelector(CollectMetadataResult.class)),
    EXTEND_METADATA_FINISHED_FAILURE_EVENT("EXTEND_METADATA_FINISHED_FAILURE_EVENT"),
    SSHFINGERPRINTS_EVENT(CloudPlatformResult.selector(GetSSHFingerprintsResult.class)),
    SSHFINGERPRINTS_FAILED_EVENT(CloudPlatformResult.failureSelector(GetSSHFingerprintsResult.class)),
    TLS_SETUP_FINISHED_EVENT("TLS_SETUP_FINISHED_EVENT"),
    TLS_SETUP_FINISHED_FAILED_EVENT("TLS_SETUP_FINISHED_EVENT"),
    BOOTSTRAP_NEW_NODES_EVENT("BOOTSTRAP_NEW_NODES"),
    BOOTSTRAP_NEW_NODES_FAILURE_EVENT(EventSelectorUtil.failureSelector(BootstrapNewNodesResult.class)),
    EXTEND_HOST_METADATA_EVENT(EventSelectorUtil.selector(BootstrapNewNodesResult.class)),
    EXTEND_HOST_METADATA_FINISHED_EVENT(EventSelectorUtil.selector(ExtendHostMetadataResult.class)),
    EXTEND_HOST_METADATA_FINISHED_FAILURE_EVENT("EXTEND_CONSUL_METADATA_FINISHED_FAILURE_EVENT"),
    EXTEND_HOST_METADATA_FAILURE_EVENT(EventSelectorUtil.failureSelector(ExtendHostMetadataResult.class)),
    MOUNT_DISKS_ON_NEW_HOSTS_EVENT(EventSelectorUtil.selector(MountDisksOnNewHostsResult.class)),
    MOUNT_DISKS_ON_NEW_HOSTS_FAILURE_EVENT(EventSelectorUtil.failureSelector(MountDisksOnNewHostsResult.class)),
    UPSCALE_FINALIZED_EVENT("UPSCALESTACKFINALIZED"),
    UPSCALE_FAIL_HANDLED_EVENT("UPSCALEFAILHANDLED");

    private final String event;

    StackUpscaleEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
