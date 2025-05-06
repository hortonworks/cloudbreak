package com.sequenceiq.freeipa.flow.freeipa.rebuild;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.AbstractRebuildAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildAddInstanceAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildBootstrapMachineAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildCleanupFreeIpaAfterRestoreAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildCollectResourcesAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildExtendMetadataAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildFinishedAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildInstallFreeIpaAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildOrchestratorConfigAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildPostInstallAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRebootAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRebootWaitUntilAvailableAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRegisterClusterProxyAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRemoveInstancesAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRemoveInstancesFinishedAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRestoreFreeIpaAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildSaveMetadataAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildStartAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildTlsSetupAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildUpdateEnvironmentStackConfigAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildUpdateKerberosNameServersConfigAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildUpdateLoadBalancerAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildUpdateMetadataForDeletionAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildValidateBackupAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildValidateCloudStorageAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildValidateHealthAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildValidateInstanceAction;

public enum FreeIpaRebuildState implements FlowState {
    INIT_STATE,
    REBUILD_START_STATE(RebuildStartAction.class),
    REBUILD_UPDATE_METADATA_FOR_DELETION_REQUEST_STATE(RebuildUpdateMetadataForDeletionAction.class),
    REBUILD_COLLECT_RESOURCES_STATE(RebuildCollectResourcesAction.class),
    REBUILD_REMOVE_INSTANCES_STATE(RebuildRemoveInstancesAction.class),
    REBUILD_REMOVE_INSTANCES_FINISHED_STATE(RebuildRemoveInstancesFinishedAction.class),
    REBUILD_ADD_INSTANCE_STATE(RebuildAddInstanceAction.class),
    REBUILD_VALIDATE_INSTANCE_STATE(RebuildValidateInstanceAction.class),
    REBUILD_EXTEND_METADATA_STATE(RebuildExtendMetadataAction.class),
    REBUILD_SAVE_METADATA_STATE(RebuildSaveMetadataAction.class),
    REBUILD_TLS_SETUP_STATE(RebuildTlsSetupAction.class),
    REBUILD_UPDATE_CLUSTERPROXY_REGISTRATION_STATE(RebuildRegisterClusterProxyAction.class),
    REBUILD_BOOTSTRAPPING_MACHINES_STATE(RebuildBootstrapMachineAction.class),
    REBUILD_ORCHESTRATOR_CONFIG_STATE(RebuildOrchestratorConfigAction.class),
    REBUILD_VALIDATE_CLOUD_STORAGE_STATE(RebuildValidateCloudStorageAction.class),
    REBUILD_VALIDATE_BACKUP_STATE(RebuildValidateBackupAction.class),
    REBUILD_FREEIPA_INSTALL_STATE(RebuildInstallFreeIpaAction.class),
    REBUILD_RESTORE_STATE(RebuildRestoreFreeIpaAction.class),
    REBUILD_REBOOT_STATE(RebuildRebootAction.class),
    REBUILD_WAIT_UNTIL_AVAILABLE_STATE(RebuildRebootWaitUntilAvailableAction.class),
    REBUILD_CLEANUP_FREEIPA_AFTER_RESTORE_STATE(RebuildCleanupFreeIpaAfterRestoreAction.class),
    REBUILD_FREEIPA_POST_INSTALL_STATE(RebuildPostInstallAction.class),
    REBUILD_VALIDATE_HEALTH_STATE(RebuildValidateHealthAction.class),
    REBUILD_UPDATE_KERBEROS_NAMESERVERS_CONFIG_STATE(RebuildUpdateKerberosNameServersConfigAction.class),
    REBUILD_UPDATE_LOAD_BALANCER_STATE(RebuildUpdateLoadBalancerAction.class),
    REBUILD_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE(RebuildUpdateEnvironmentStackConfigAction.class),
    REBUILD_FINISHED_STATE(RebuildFinishedAction.class),
    REBUILD_FAILED_STATE(RebuildFailedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractRebuildAction<?>> action;

    FreeIpaRebuildState(Class<? extends AbstractRebuildAction<?>> action) {
        this.action = action;
    }

    FreeIpaRebuildState() {
    }

    @Override
    public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
