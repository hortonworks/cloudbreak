package com.sequenceiq.freeipa.flow.freeipa.upscale;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationSuccess;

public enum UpscaleFlowEvent implements FlowEvent {
    UPSCALE_EVENT("UPSCALE_EVENT"),
    UPSCALE_STARTING_FINISHED_EVENT("UPSCALE_STARTING_FINISHED_EVENT"),
    UPSCALE_ADD_INSTANCES_FINISHED_EVENT(CloudPlatformResult.selector(UpscaleStackResult.class)),
    UPSCALE_ADD_INSTANCES_FAILED_EVENT(CloudPlatformResult.failureSelector(UpscaleStackResult.class)),
    UPSCALE_VALIDATE_INSTANCES_FINISHED_EVENT("UPSCALE_VALIDATE_INSTANCES_FINISHED_EVENT"),
    UPSCALE_VALIDATE_INSTANCES_FAILED_EVENT("UPSCALE_VALIDATE_INSTANCES_FAILED_EVENT"),
    UPSCALE_EXTEND_METADATA_FINISHED_EVENT(CloudPlatformResult.selector(CollectMetadataResult.class)),
    UPSCALE_EXTEND_METADATA_FAILED_EVENT(CloudPlatformResult.failureSelector(CollectMetadataResult.class)),
    UPSCALE_SAVE_METADATA_FINISHED_EVENT("UPSCALE_SAVE_METADATA_FINISHED_EVENT"),
    UPSCALE_TLS_SETUP_FINISHED_EVENT("UPSCALE_TLS_SETUP_FINISHED_EVENT"),
    UPSCALE_TLS_SETUP_FAILED_EVENT(EventSelectorUtil.selector(UpscaleFailureEvent.class)),
    UPSCALE_CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterProxyRegistrationSuccess.class)),
    UPSCALE_CLUSTER_PROXY_REGISTRATION_FAILED_EVENT(EventSelectorUtil.selector(ClusterProxyRegistrationFailed.class)),
    UPSCALE_BOOTSTRAP_MACHINES_FINISHED_EVENT(EventSelectorUtil.selector(BootstrapMachinesSuccess.class)),
    UPSCALE_BOOTSTRAP_MACHINES_FAILED_EVENT(EventSelectorUtil.selector(BootstrapMachinesFailed.class)),
    UPSCALE_HOST_METADATASETUP_FINISHED_EVENT(EventSelectorUtil.selector(HostMetadataSetupSuccess.class)),
    UPSCALE_HOST_METADATASETUP_FAILED_EVENT(EventSelectorUtil.selector(HostMetadataSetupFailed.class)),
    UPSCALE_RECORD_HOSTNAMES_FINISHED_EVENT("UPSCALE_RECORD_HOSTNAMES_FINISHED_EVENT"),
    UPSCALE_ORCHESTRATOR_CONFIG_FINISHED_EVENT(EventSelectorUtil.selector(OrchestratorConfigSuccess.class)),
    UPSCALE_ORCHESTRATOR_CONFIG_FAILED_EVENT(EventSelectorUtil.selector(OrchestratorConfigFailed.class)),
    UPSCALE_VALIDATING_CLOUD_STORAGE_FINISHED_EVENT(EventSelectorUtil.selector(ValidateCloudStorageSuccess.class)),
    UPSCALE_VALIDATING_CLOUD_STORAGE_FAILED_EVENT(EventSelectorUtil.selector(ValidateCloudStorageFailed.class)),
    UPSCALE_FREEIPA_INSTALL_FINISHED_EVENT(EventSelectorUtil.selector(InstallFreeIpaServicesSuccess.class)),
    UPSCALE_FREEIPA_INSTALL_FAILED_EVENT(EventSelectorUtil.selector(InstallFreeIpaServicesFailed.class)),
    UPSCALE_UPDATE_CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterProxyUpdateRegistrationSuccess.class)),
    UPSCALE_UPDATE_CLUSTER_PROXY_REGISTRATION_FAILED_EVENT(EventSelectorUtil.selector(ClusterProxyUpdateRegistrationFailed.class)),
    UPSCALE_FREEIPA_POST_INSTALL_FINISHED_EVENT(EventSelectorUtil.selector(PostInstallFreeIpaSuccess.class)),
    UPSCALE_FREEIPA_POST_INSTALL_FAILED_EVENT(EventSelectorUtil.selector(PostInstallFreeIpaFailed.class)),
    UPSCALE_UPDATE_METADATA_FINISHED_EVENT("UPSCALE_UPDATE_METADATA_FINISHED_EVENT"),
    UPSCALE_VALIDATE_NEW_INSTANCES_HEALTH_FINISHED_EVENT,
    UPSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT("UPSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT"),
    UPSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT("UPSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT"),
    UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT("UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT"),
    UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT("UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT"),
    UPSCALE_FINISHED_EVENT("UPSCALE_FINISHED_EVENT"),
    FAILURE_EVENT("UPSCALE_FAILURE_EVENT"),
    FAIL_HANDLED_EVENT("UPSCALE_FAIL_HANDLED_EVENT");

    private final String event;

    UpscaleFlowEvent(String event) {
        this.event = event;
    }

    UpscaleFlowEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
