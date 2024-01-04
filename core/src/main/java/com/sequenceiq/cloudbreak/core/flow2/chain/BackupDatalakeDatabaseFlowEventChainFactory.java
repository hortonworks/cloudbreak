package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.BACKUP_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.BACKUP_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.BACKUP_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_FINISHED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseBackupTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class BackupDatalakeDatabaseFlowEventChainFactory implements FlowEventChainFactory<DatabaseBackupTriggerEvent>, ClusterUseCaseAware {
    private static final Logger LOGGER = getLogger(BackupDatalakeDatabaseFlowEventChainFactory.class);

    @Inject
    private StackService stackService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_DATABASE_BACKUP_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DatabaseBackupTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        StackStatus stackStatus = stackService.getCurrentStatusByStackId(event.getResourceId());
        if (!stackStatus.getDetailedStackStatus().equals(DETERMINE_DATALAKE_DATA_SIZES_FINISHED)) {
            LOGGER.info("Adding salt update step to backup DL DB flow chain.");
            flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        } else {
            LOGGER.info("Skipping salt update step in backup DL DB flow chain due to already being done as part of determine DL data sizes flow chain.");
        }
        flowEventChain.add(new DatabaseBackupTriggerEvent(DATABASE_BACKUP_EVENT.event(), event.getResourceId(), event.accepted(),
                event.getBackupLocation(), event.getBackupId(), event.isCloseConnections(), event.getSkipDatabaseNames(),
                event.getDatabaseMaxDurationInMin()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public Value getUseCaseForFlowState(Enum flowState) {
        if (SaltUpdateState.INIT_STATE.equals(flowState)) {
            return BACKUP_STARTED;
        } else if (DatabaseBackupState.DATABASE_BACKUP_FINISHED_STATE.equals(flowState)) {
            return BACKUP_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return BACKUP_FAILED;
        } else {
            return UNSET;
        }
    }
}
