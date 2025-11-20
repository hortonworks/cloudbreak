package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackHandlerSelectors.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackHandlerSelectors.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FINALIZE_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.HANDLED_FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_ROLLBACK_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_ROLLBACK_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_ROLLBACK_STARTED_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackTriggerEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class MigrateZookeeperToKraftRollbackActions {
    private static final Logger LOGGER = getLogger(MigrateZookeeperToKraftRollbackActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Bean(name = "ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_STATE")
    public Action<?, ?> rollbackZookeeperToKraftMigrationValidationAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftRollbackTriggerEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftRollbackTriggerEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftRollbackTriggerEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.debug("Rollback Zookeeper to KRaft migration validation state started {}", payload);
                Long stackId = payload.getResourceId();
                String nextEvent = ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftRollbackEvent(nextEvent, payload.getResourceId()));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftRollbackTriggerEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftRollbackFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_STATE")
    public Action<?, ?> rollbackZookeeperToKraftMigrationAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftRollbackEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftRollbackEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftRollbackEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.debug("Rollback Zookeeper to KRaft migration state started {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS);
                flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_KRAFT_MIGRATION_ROLLBACK_STARTED_EVENT);
                String nextEvent = ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftRollbackEvent(nextEvent, payload.getResourceId()));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftRollbackEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftRollbackFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE")
    public Action<?, ?> rollbackZookeeperToKraftMigrationFinished() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftRollbackEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftRollbackEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftRollbackEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Rollback Zookeeper to KRaft migration finished state started {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE);
                flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_KRAFT_MIGRATION_ROLLBACK_FINISHED_EVENT);
                String nextEvent = FINALIZE_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftRollbackEvent(nextEvent, payload.getResourceId()));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftRollbackEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftRollbackFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED_STATE")
    public Action<?, ?> rollbackZookeeperToKraftMigrationFailed() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftRollbackFailureEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftRollbackFailureEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftRollbackFailureEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.error("Rollback Zookeeper to KRaft migration failed: {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED);
                flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_KRAFT_MIGRATION_ROLLBACK_FAILED_EVENT,
                        payload.getException().getMessage());
                sendEvent(context, HANDLED_FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftRollbackFailureEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftRollbackFailureEvent(payload.getResourceId(), ex);
            }
        };
    }
}