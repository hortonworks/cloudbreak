package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpdateDomainDnsResolverResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackImageFallbackResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackSaltValidationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleUpdateUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleUpdateUserdataSecretsSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum StackUpscaleEvent implements FlowEvent {
    ADD_INSTANCES_EVENT("STACK_UPSCALE_TRIGGER_EVENT"),
    UPSCALE_VALID_EVENT(CloudPlatformResult.selector(UpscaleStackValidationResult.class)),
    UPSCALE_INVALID_EVENT(CloudPlatformResult.failureSelector(UpscaleStackValidationResult.class)),
    UPSCALE_SALT_VALID_EVENT(CloudPlatformResult.selector(UpscaleStackSaltValidationResult.class)),
    UPSCALE_SALT_INVALID_EVENT(CloudPlatformResult.failureSelector(UpscaleStackSaltValidationResult.class)),
    UPSCALE_CREATE_USERDATA_SECRETS_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleCreateUserdataSecretsSuccess.class)),
    UPSCALE_CREATE_USERDATA_SECRETS_FAILED_EVENT(EventSelectorUtil.selector(UpscaleCreateUserdataSecretsFailed.class)),
    UPDATE_DOMAIN_DNS_RESOLVER_FINISHED_EVENT(EventSelectorUtil.selector(UpdateDomainDnsResolverResult.class)),
    UPDATE_DOMAIN_DNS_RESOLVER_FAILED_EVENT(EventSelectorUtil.failureSelector(UpdateDomainDnsResolverResult.class)),
    ADD_INSTANCES_FINISHED_EVENT(CloudPlatformResult.selector(UpscaleStackResult.class)),

    UPSCALE_IMAGE_FALLBACK_EVENT(CloudPlatformResult.selector(UpscaleStackImageFallbackResult.class)),
    UPSCALE_IMAGE_FALLBACK_FINISHED_EVENT(CloudPlatformResult.selector(ImageFallbackSuccess.class)),
    UPSCALE_IMAGE_FALLBACK_FAILED_EVENT(CloudPlatformResult.selector(ImageFallbackFailed.class)),
    ADD_INSTANCES_FAILURE_EVENT(CloudPlatformResult.failureSelector(UpscaleStackResult.class)),
    ADD_INSTANCES_FINISHED_FAILURE_EVENT("ADD_INSTANCES_FINISHED_FAILURE_EVENT"),
    EXTEND_METADATA_EVENT("EXTEND_METADATA"),
    EXTEND_METADATA_FINISHED_EVENT(CloudPlatformResult.selector(CollectMetadataResult.class)),
    EXTEND_METADATA_FAILURE_EVENT(CloudPlatformResult.failureSelector(CollectMetadataResult.class)),
    EXTEND_METADATA_FINISHED_FAILURE_EVENT("EXTEND_METADATA_FINISHED_FAILURE_EVENT"),
    UPSCALE_UPDATE_LOAD_BALANCERS_EVENT("UPSCALE_UPDATE_LOAD_BALANCERS_EVENT"),
    UPSCALE_UPDATE_LOAD_BALANCERS_FAILURE_EVENT("UPSCALE_UPDATE_LOAD_BALANCERS_FAILURE_EVENT"),
    UPSCALE_UPDATE_USERDATA_SECRETS_EVENT("UPSCALE_UPDATE_USERDATA_SECRETS_EVENT"),
    UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleUpdateUserdataSecretsSuccess.class)),
    UPSCALE_UPDATE_USERDATA_SECRETS_FAILURE_EVENT(EventSelectorUtil.selector(UpscaleUpdateUserdataSecretsFailed.class)),
    UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_FAILURE_EVENT("UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_FAILURE_EVENT"),
    SSHFINGERPRINTS_EVENT(CloudPlatformResult.selector(GetSSHFingerprintsResult.class)),
    SSHFINGERPRINTS_FAILED_EVENT(CloudPlatformResult.failureSelector(GetSSHFingerprintsResult.class)),
    TLS_SETUP_FINISHED_EVENT("TLS_SETUP_FINISHED_EVENT"),
    TLS_SETUP_FINISHED_FAILED_EVENT("TLS_SETUP_FINISHED_EVENT"),
    CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterProxyReRegistrationResult.class)),
    CLUSTER_PROXY_RE_REGISTRATION_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterProxyReRegistrationResult.class)),
    BOOTSTRAP_NEW_NODES_EVENT("BOOTSTRAP_NEW_NODES"),
    BOOTSTRAP_NEW_NODES_FAILURE_EVENT(EventSelectorUtil.failureSelector(BootstrapNewNodesResult.class)),
    EXTEND_HOST_METADATA_EVENT(EventSelectorUtil.selector(BootstrapNewNodesResult.class)),
    EXTEND_HOST_METADATA_FINISHED_EVENT(EventSelectorUtil.selector(ExtendHostMetadataResult.class)),
    EXTEND_HOST_METADATA_FINISHED_FAILURE_EVENT("EXTEND_CONSUL_METADATA_FINISHED_FAILURE_EVENT"),
    EXTEND_HOST_METADATA_FAILURE_EVENT(EventSelectorUtil.failureSelector(ExtendHostMetadataResult.class)),
    CLEANUP_FREEIPA_FINISHED_EVENT("CLEANUP_FREEIPA_FINISHED"),
    CLEANUP_FREEIPA_FAILED_EVENT("CLEANUP_FREEIPA_FAILED_EVENT"),
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
