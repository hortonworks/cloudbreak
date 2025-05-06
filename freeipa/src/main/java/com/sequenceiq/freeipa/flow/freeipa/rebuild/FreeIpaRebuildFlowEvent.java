package com.sequenceiq.freeipa.flow.freeipa.rebuild;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.stack.HealthCheckFailed;
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationSuccess;

public enum FreeIpaRebuildFlowEvent implements FlowEvent {
    REBUILD_EVENT(EventSelectorUtil.selector(RebuildEvent.class)),
    REBUILD_STARTED_EVENT,
    UPDATE_METADATA_FOR_DELETION_FINISHED_EVENT,
    COLLECT_RESOURCES_FINISHED_EVENT(EventSelectorUtil.selector(DownscaleStackCollectResourcesResult.class)),
    COLLECT_RESOURCES_FAILED_EVENT(EventSelectorUtil.failureSelector(DownscaleStackCollectResourcesResult.class)),
    REMOVE_INSTANCES_FINISHED_EVENT(EventSelectorUtil.selector(DownscaleStackResult.class)),
    REMOVE_INSTANCES_FAILED_EVENT(EventSelectorUtil.failureSelector(DownscaleStackResult.class)),
    ADD_INSTANCE_EVENT,
    ADD_INSTANCE_FINISHED_EVENT(CloudPlatformResult.selector(UpscaleStackResult.class)),
    ADD_INSTANCE_FAILED_EVENT(CloudPlatformResult.failureSelector(UpscaleStackResult.class)),
    VALIDATE_INSTANCE_FINISHED_EVENT,
    EXTEND_METADATA_FINISHED_EVENT(CloudPlatformResult.selector(CollectMetadataResult.class)),
    EXTEND_METADATA_FAILED_EVENT(CloudPlatformResult.failureSelector(CollectMetadataResult.class)),
    SAVE_METADATA_FINISHED_EVENT,
    TLS_SETUP_FINISHED_EVENT,
    CLUSTER_PROXY_REGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterProxyRegistrationSuccess.class)),
    CLUSTER_PROXY_REGISTRATION_FAILED_EVENT(EventSelectorUtil.selector(ClusterProxyRegistrationFailed.class)),
    BOOTSTRAP_MACHINES_FINISHED_EVENT(EventSelectorUtil.selector(BootstrapMachinesSuccess.class)),
    BOOTSTRAP_MACHINES_FAILED_EVENT(EventSelectorUtil.selector(BootstrapMachinesFailed.class)),
    ORCHESTRATOR_CONFIG_FINISHED_EVENT(EventSelectorUtil.selector(OrchestratorConfigSuccess.class)),
    ORCHESTRATOR_CONFIG_FAILED_EVENT(EventSelectorUtil.selector(OrchestratorConfigFailed.class)),
    VALIDATING_CLOUD_STORAGE_FINISHED_EVENT(EventSelectorUtil.selector(ValidateCloudStorageSuccess.class)),
    VALIDATING_CLOUD_STORAGE_FAILED_EVENT(EventSelectorUtil.selector(ValidateCloudStorageFailed.class)),
    VALIDATING_BACKUP_FINISHED_EVENT(EventSelectorUtil.selector(ValidateBackupSuccess.class)),
    VALIDATING_BACKUP_FAILED_EVENT(EventSelectorUtil.selector(ValidateBackupFailed.class)),
    FREEIPA_INSTALL_FINISHED_EVENT(EventSelectorUtil.selector(InstallFreeIpaServicesSuccess.class)),
    FREEIPA_INSTALL_FAILED_EVENT(EventSelectorUtil.selector(InstallFreeIpaServicesFailed.class)),
    FREEIPA_RESTORE_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaRestoreSuccess.class)),
    FREEIPA_RESTORE_FAILED_EVENT(EventSelectorUtil.selector(FreeIpaRestoreFailed.class)),
    REBOOT_TRIGGERED_EVENT(CloudPlatformResult.selector(RebootInstancesResult.class)),
    REBOOT_TRIGGER_FAILURE_EVENT(CloudPlatformResult.failureSelector(RebootInstancesResult.class)),
    REBOOT_WAIT_UNTIL_AVAILABLE_FINISHED_EVENT(EventSelectorUtil.selector(HealthCheckSuccess.class)),
    REBOOT_WAIT_UNTIL_AVAILABLE_FAILURE_EVENT(EventSelectorUtil.selector(HealthCheckFailed.class)),
    FREEIPA_CLEANUP_AFTER_RESTORE_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaCleanupAfterRestoreSuccess.class)),
    FREEIPA_CLEANUP_AFTER_RESTORE_FAILED_EVENT(EventSelectorUtil.selector(FreeIpaCleanupAfterRestoreFailed.class)),
    FREEIPA_POST_INSTALL_FINISHED_EVENT(EventSelectorUtil.selector(PostInstallFreeIpaSuccess.class)),
    FREEIPA_POST_INSTALL_FAILED_EVENT(EventSelectorUtil.selector(PostInstallFreeIpaFailed.class)),
    VALIDATE_HEALTH_FINISHED_EVENT(EventSelectorUtil.selector(RebuildValidateHealthSuccess.class)),
    VALIDATE_HEALTH_FAILED_EVENT(EventSelectorUtil.selector(RebuildValidateHealthFailed.class)),
    UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT,
    UPDATE_LOAD_BALANCER_FINISHED_EVENT(EventSelectorUtil.selector(LoadBalancerUpdateSuccess.class)),
    UPDATE_LOAD_BALANCER_FAILED_EVENT(EventSelectorUtil.selector(LoadBalancerUpdateFailureEvent.class)),
    UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT,
    REBUILD_FINISHED_EVENT,
    REBUILD_FAILURE_EVENT(EventSelectorUtil.selector(RebuildFailureEvent.class)),
    REBUILD_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaRebuildFlowEvent(String event) {
        this.event = event;
    }

    FreeIpaRebuildFlowEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }

    @Override
    public String selector() {
        return event;
    }
}
