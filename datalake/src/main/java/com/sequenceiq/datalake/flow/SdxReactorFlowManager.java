package com.sequenceiq.datalake.flow;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RESIZE_TRIGGERED;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent.SDX_RESIZE_FLOW_CHAIN_START_EVENT;
import static com.sequenceiq.datalake.flow.detach.event.DatalakeResizeRecoveryFlowChainStartEvent.SDX_RESIZE_RECOVERY_FLOW_CHAIN_START_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxCmDiagnosticsEvent.SDX_CM_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsEvent.SDX_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_TRIGGER_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_UPGRADE_STACK_EVENT;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxStartCertRenewalEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxStartCertRotationEvent;
import com.sequenceiq.datalake.flow.datalake.cmsync.event.SdxCmSyncStartEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryStartEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeStartEvent;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeRecoveryFlowChainStartEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxCmDiagnosticsCollectionEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsCollectionEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeTriggerBackupEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeTriggerRestoreEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairStartEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartStartEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStartStopEvent;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmStackEvent;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.settings.SdxRepairSettings;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.FlowNameFormatService;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class SdxReactorFlowManager {

    private static final long WAIT_FOR_ACCEPT = 10L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxReactorFlowManager.class);

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private FlowNameFormatService flowNameFormatService;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    public FlowIdentifier triggerSdxCreation(SdxCluster cluster) {
        LOGGER.info("Trigger Datalake creation for: {}", cluster);
        String selector = SDX_VALIDATION_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxEvent(selector, cluster.getId(), userId), cluster.getClusterName());
    }

    public FlowIdentifier triggerSdxResize(Long sdxClusterId, SdxCluster newSdxCluster) {
        LOGGER.info("Trigger Datalake resizing for: {}", sdxClusterId);
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        boolean performBackup = sdxBackupRestoreService.shouldSdxBackupBePerformed(
                newSdxCluster, entitlementService.isDatalakeBackupOnResizeEnabled(ThreadBasedUserCrnProvider.getAccountId())
        );
        boolean performRestore = sdxBackupRestoreService.shouldSdxRestoreBePerformed(
                newSdxCluster, entitlementService.isDatalakeBackupOnResizeEnabled(ThreadBasedUserCrnProvider.getAccountId())
        );
        eventSenderService.sendEventAndNotification(newSdxCluster, userId, DATALAKE_RESIZE_TRIGGERED);
        return notify(SDX_RESIZE_FLOW_CHAIN_START_EVENT, new DatalakeResizeFlowChainStartEvent(sdxClusterId, newSdxCluster, userId,
                environmentClientService.getBackupLocation(newSdxCluster.getEnvCrn()), performBackup, performRestore), newSdxCluster.getClusterName());
    }

    public FlowIdentifier triggerSdxResizeRecovery(SdxCluster oldSdxCluster, SdxCluster newSdxCluster) {
        LOGGER.info("Triggering recovery for failed SDX resize with original cluster: {} and resized cluster: {}",
                oldSdxCluster, newSdxCluster);
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(
                SDX_RESIZE_RECOVERY_FLOW_CHAIN_START_EVENT,
                new DatalakeResizeRecoveryFlowChainStartEvent(oldSdxCluster, newSdxCluster, userId),
                oldSdxCluster.getClusterName()
        );
    }

    public FlowIdentifier triggerSdxDeletion(SdxCluster cluster, boolean forced) {
        LOGGER.info("Trigger Datalake deletion for: {} forced: {}", cluster, forced);
        String selector = SDX_DELETE_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxDeleteStartEvent(selector, cluster.getId(), userId, forced), cluster.getClusterName());
    }

    public FlowIdentifier triggerSdxRepairFlow(SdxCluster cluster, SdxRepairRequest repairRequest) {
        LOGGER.info("Trigger Datalake repair for: {} with settings: {}", cluster, repairRequest);
        SdxRepairSettings settings = SdxRepairSettings.from(repairRequest);
        String selector = SDX_REPAIR_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxRepairStartEvent(selector, cluster.getId(), userId, settings), cluster.getClusterName());
    }

    public FlowIdentifier triggerDatalakeRuntimeUpgradeFlow(SdxCluster cluster, String imageId, SdxUpgradeReplaceVms replaceVms, boolean skipBackup) {
        LOGGER.info("Trigger Datalake runtime upgrade for: {} with imageId: {} and replace vm param: {}", cluster, imageId, replaceVms);
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        if (!skipBackup && sdxBackupRestoreService.shouldSdxBackupBePerformed(
                cluster, entitlementService.isDatalakeBackupOnUpgradeEnabled(ThreadBasedUserCrnProvider.getAccountId())
        )) {
            LOGGER.info("Triggering backup before an upgrade");
            return notify(DatalakeUpgradeFlowChainStartEvent.DATALAKE_UPGRADE_FLOW_CHAIN_EVENT,
                    new DatalakeUpgradeFlowChainStartEvent(DatalakeUpgradeFlowChainStartEvent.DATALAKE_UPGRADE_FLOW_CHAIN_EVENT, cluster.getId(),
                            userId, imageId, replaceVms.getBooleanValue(), environmentClientService.getBackupLocation(cluster.getEnvCrn())),
                    cluster.getClusterName());
        } else {
            return notify(DATALAKE_UPGRADE_EVENT.event(), new DatalakeUpgradeStartEvent(DATALAKE_UPGRADE_EVENT.event(), cluster.getId(),
                    userId, imageId, replaceVms.getBooleanValue()), cluster.getClusterName());
        }
    }

    public FlowIdentifier triggerDatalakeSyncComponentVersionsFromCmFlow(SdxCluster cluster) {
        LOGGER.info("Trigger Datalake sync component versions from CM");
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCmSyncStartEvent event = new SdxCmSyncStartEvent(cluster.getId(), userId);
        return notify(event.selector(), event, cluster.getClusterName());
    }

    public FlowIdentifier triggerDatalakeRuntimeRecoveryFlow(SdxCluster cluster, SdxRecoveryType recoveryType) {
        LOGGER.info("Trigger recovery of failed runtime upgrade for: {} with recovery type: {}", cluster, recoveryType);
        String selector = DATALAKE_RECOVERY_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new DatalakeRecoveryStartEvent(selector, cluster.getId(), userId, recoveryType), cluster.getClusterName());
    }

    public FlowIdentifier triggerSdxStartFlow(SdxCluster cluster) {
        LOGGER.info("Trigger Datalake start for: {}", cluster);
        String selector = SDX_START_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxStartStartEvent(selector, cluster.getId(), userId), cluster.getClusterName());
    }

    public FlowIdentifier triggerSdxStopFlow(SdxCluster cluster) {
        LOGGER.info("Trigger Datalake stop for: {}", cluster);
        String selector = SDX_STOP_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxStartStopEvent(selector, cluster.getId(), userId), cluster.getClusterName());
    }

    public FlowIdentifier triggerDatalakeDatabaseBackupFlow(DatalakeDatabaseBackupStartEvent startEvent, String identifier) {
        String selector = DATALAKE_DATABASE_BACKUP_EVENT.event();
        return notify(selector, startEvent, identifier);
    }

    public FlowIdentifier triggerDatalakeBackupFlow(DatalakeTriggerBackupEvent startEvent, String identifier) {
        String selector = DATALAKE_TRIGGER_BACKUP_EVENT.event();
        return notify(selector, startEvent, identifier);
    }

    public FlowIdentifier triggerDatalakeDatabaseRestoreFlow(DatalakeDatabaseRestoreStartEvent startEvent, String identifier) {
        String selector = DATALAKE_DATABASE_RESTORE_EVENT.event();
        return notify(selector, startEvent, identifier);
    }

    public FlowIdentifier triggerDatalakeRestoreFlow(DatalakeTriggerRestoreEvent startEvent, String identifier) {
        String selector = DATALAKE_TRIGGER_RESTORE_EVENT.event();
        return notify(selector, startEvent, identifier);
    }

    public FlowIdentifier triggerDiagnosticsCollection(SdxDiagnosticsCollectionEvent startEvent, String identifier) {
        String selector = SDX_DIAGNOSTICS_COLLECTION_EVENT.event();
        return notify(selector, startEvent, identifier);
    }

    public FlowIdentifier triggerCmDiagnosticsCollection(SdxCmDiagnosticsCollectionEvent startEvent, String identifier) {
        String selector = SDX_CM_DIAGNOSTICS_COLLECTION_EVENT.event();
        return notify(selector, startEvent, identifier);
    }

    public FlowIdentifier triggerCertRotation(SdxStartCertRotationEvent event, String identifier) {
        return notify(event.selector(), event, identifier);
    }

    public FlowIdentifier triggerCertRenewal(SdxStartCertRenewalEvent event, String identifier) {
        return notify(event.selector(), event, identifier);
    }

    public FlowIdentifier triggerCcmUpgradeFlow(SdxCluster cluster) {
        LOGGER.info("Trigger CCM Upgrade on Datalake for: {}", cluster);
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        UpgradeCcmStackEvent event = new UpgradeCcmStackEvent(UPGRADE_CCM_UPGRADE_STACK_EVENT.event(), cluster.getId(), initiatorUserCrn);
        return notify(event.selector(), event, cluster.getClusterName());
    }

    private FlowIdentifier notify(String selector, SdxEvent acceptable, String identifier) {
        Map<String, Object> flowTriggerUserCrnHeader = Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, acceptable.getUserId());
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(flowTriggerUserCrnHeader, acceptable);

        reactor.notify(selector, event);
        try {
            FlowAcceptResult accepted = (FlowAcceptResult) event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            if (accepted == null) {
                throw new FlowNotAcceptedException(String.format("Timeout happened when trying to start the flow for sdx cluster %s.",
                        event.getData().getResourceId()));
            } else {
                switch (accepted.getResultType()) {
                    case ALREADY_EXISTING_FLOW:
                        throw new FlowsAlreadyRunningException(String.format("Request not allowed, datalake cluster '%s' already has a running operation. " +
                                        "Running operation(s): [%s]",
                                identifier,
                                flowNameFormatService.formatFlows(accepted.getAlreadyRunningFlows())));
                    case RUNNING_IN_FLOW:
                        return new FlowIdentifier(FlowType.FLOW, accepted.getAsFlowId());
                    case RUNNING_IN_FLOW_CHAIN:
                        return new FlowIdentifier(FlowType.FLOW_CHAIN, accepted.getAsFlowChainId());
                    default:
                        throw new IllegalStateException("Unsupported accept result type: " + accepted.getClass());
                }
            }
        } catch (InterruptedException e) {
            throw new CloudbreakApiException(e.getMessage());
        }
    }

}
