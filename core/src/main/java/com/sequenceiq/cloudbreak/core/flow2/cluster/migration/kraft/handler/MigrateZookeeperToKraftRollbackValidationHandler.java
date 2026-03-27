package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackHandlerSelectors.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_ROLLBACK_SKIPPED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationOperationStatusFactory;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateZookeeperToKraftRollbackValidationHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftRollbackEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftRollbackValidationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private KraftMigrationService kraftMigrationService;

    @Inject
    private KraftMigrationOperationStatusFactory kraftMigrationOperationStatusFactory;

    @Inject
    private FlowMessageService flowMessageService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftRollbackEvent> event) {
        LOGGER.error("Rollback Zookeeper to KRaft migration validation failed.", e);
        return new MigrateZookeeperToKraftRollbackFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stack = stackDtoService.getById(stackId);
        try {
            KraftMigrationStatus kraftMigrationStatus = kraftMigrationService.getKraftMigrationStatus(stack);
            boolean rollbackCompleted = kraftMigrationOperationStatusFactory.getStatusFromFlowInformation(stack)
                    .filter(KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE::equals)
                    .isPresent();
            boolean statusIsNotRollbackable = KraftMigrationStatus.ZOOKEEPER_INSTALLED.equals(kraftMigrationStatus)
                    || KraftMigrationStatus.KRAFT_INSTALLED.equals(kraftMigrationStatus);
            if (rollbackCompleted || statusIsNotRollbackable) {
                String skipReason = String.format("Skipping rollback Zookeeper to KRaft migration because rollback is executed recently or it is not in a" +
                        " rollbackable state: [rollbackCompleted=%s], [statusIsNotRollbackable=%s]", rollbackCompleted, statusIsNotRollbackable);
                LOGGER.debug(skipReason);
                flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_KRAFT_MIGRATION_ROLLBACK_SKIPPED_EVENT, skipReason);
                return new MigrateZookeeperToKraftRollbackEvent(FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), stackId);
            }
        } catch (Exception e) {
            LOGGER.error("Rollback Zookeeper to KRaft migration validation failed.", e);
            return new MigrateZookeeperToKraftRollbackFailureEvent(stackId, e);
        }
        return new MigrateZookeeperToKraftRollbackEvent(START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), stackId);
    }

    @Override
    public String selector() {
        return ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.selector();
    }
}