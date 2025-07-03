package com.sequenceiq.datalake.flow.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EVENT;
import static com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent.SDX_RESIZE_FLOW_CHAIN_START_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_TRIGGER_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationState.DATALAKE_BACKUP_VALIDATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationState.DATALAKE_RESTORE_VALIDATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_IPA_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_EVENT;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.DatalakeUseCaseAware;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeTriggerBackupEvent;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFailureReason;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeTriggerRestoreEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerBackupValidationEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerRestoreValidationEvent;
import com.sequenceiq.datalake.flow.loadbalancer.dns.event.StartUpdateLoadBalancerDNSEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshStartEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStartStopEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitState;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class DatalakeResizeFlowEventChainFactory implements FlowEventChainFactory<DatalakeResizeFlowChainStartEvent>, DatalakeUseCaseAware {

    private static final boolean STOP_DATAHUBS = false;

    @Override
    public String initEvent() {
        return SDX_RESIZE_FLOW_CHAIN_START_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DatalakeResizeFlowChainStartEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();
        chain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));

        if (!event.getSkipOptions().isSkipValidation()) {
            chain.add(new DatalakeTriggerBackupValidationEvent(DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT.event(),
                    event.getResourceId(), event.getUserId(), event.getBackupLocation(), DatalakeBackupFailureReason.BACKUP_ON_RESIZE));

            chain.add(new DatalakeTriggerRestoreValidationEvent(DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT.event(),
                    event.getResourceId(), event.getUserId(), event.getBackupLocation(),
                    DatalakeRestoreFailureReason.RESTORE_ON_RESIZE));
            if (event.isValidationOnly()) {
                chain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
                return new FlowTriggerEventQueue(getName(), event, chain);
            }
        }

        if (event.shouldTakeBackup()) {
            // Take a backup
            chain.add(new DatalakeTriggerBackupEvent(DATALAKE_TRIGGER_BACKUP_EVENT.event(),
                    event.getResourceId(), event.getUserId(), event.getBackupLocation(), "resize" + System.currentTimeMillis(),
                    event.getSkipOptions(), DatalakeBackupFailureReason.BACKUP_ON_RESIZE, Collections.emptyList(), 0));
        }

        // Stop datalake
        chain.add(new SdxStartStopEvent(SDX_STOP_EVENT.event(), event.getResourceId(), event.getUserId(), STOP_DATAHUBS));

        // Detach sdx from environment
        chain.add(new SdxStartDetachEvent(SDX_DETACH_EVENT.event(), event.getResourceId(), event.getSdxCluster(), event.getUserId()));

        // Create new
        chain.add(new SdxEvent(SDX_VALIDATION_EVENT.event(), event.getResourceId(), event.getSdxCluster().getClusterName(), event.getUserId()));

        if (event.shouldPerformRestore()) {
            //restore the new cluster
            chain.add(new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), event.getResourceId(), event.getSdxCluster().getClusterName(),
                    event.getUserId(), null, event.getBackupLocation(), null, event.getSkipOptions(),
                    DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, false));
        }

        // Delete the detached Sdx
        chain.add(new SdxDeleteStartEvent(SDX_DELETE_EVENT.event(), event.getResourceId(), event.getUserId(), true));

        // Update FreeIPA LB DNS entry in case it was deleted during deletion of other DL with same LB endpoint.
        chain.add(new StartUpdateLoadBalancerDNSEvent(UPDATE_LOAD_BALANCER_DNS_IPA_EVENT.event(), event.getResourceId(),
                event.getSdxCluster().getClusterName(), event.getUserId()));

        chain.add(new DatahubRefreshStartEvent(event.getResourceId(), event.getSdxCluster().getClusterName(), event.getUserId()));

        chain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));

        return new FlowTriggerEventQueue(getName(), event, chain);
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (FlowChainInitState.INIT_STATE.equals(flowState)) {
            return Value.DATALAKE_RESIZE_STARTED;
        } else if (FlowChainFinalizeState.FLOWCHAIN_FINALIZE_FINISHED_STATE.equals(flowState)) {
            return Value.DATALAKE_RESIZE_FINISHED;
        } else if (flowState.name().equals(DATALAKE_BACKUP_VALIDATION_FAILED_STATE.name())
            || flowState.name().equals(DATALAKE_RESTORE_VALIDATION_FAILED_STATE.name())) {
            return Value.DATALAKE_RESIZE_VALIDATION_FAILED;
        } else if (flowState.toString().contains("_FAIL")) {
            return Value.DATALAKE_RESIZE_FAILED;
        } else {
            return Value.UNSET;
        }
    }
}
