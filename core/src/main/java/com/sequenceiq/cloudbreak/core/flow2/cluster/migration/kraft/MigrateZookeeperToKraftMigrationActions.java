package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_BROKER_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_CONNECT_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_COMMAND_IN_PROGRESS_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FINISHED_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftTriggerEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class MigrateZookeeperToKraftMigrationActions {
    private static final Logger LOGGER = getLogger(MigrateZookeeperToKraftMigrationActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE")
    public Action<?, ?> migrateZookeeperToKraftValidationAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftTriggerEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftTriggerEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft validation state started {}", payload);
                Long stackId = payload.getResourceId();
                String nextEvent = MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftEvent(nextEvent, stackId));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftTriggerEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "RESTART_KAFKA_BROKER_NODES_STATE")
    public Action<?, ?> restartKafkaBrokerNodesAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft restart Kafka broker nodes state started {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS);
                flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_KRAFT_MIGRATION_COMMAND_IN_PROGRESS_EVENT);
                String nextEvent = RESTART_KAFKA_BROKER_NODES_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftEvent(nextEvent, stackId));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "RESTART_KAFKA_CONNECT_NODES_STATE")
    public Action<?, ?> restartKafkaConnectNodesAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft restart Kafka connect nodes state started {}", payload);
                Long stackId = payload.getResourceId();
                String nextEvent = RESTART_KAFKA_CONNECT_NODES_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftEvent(nextEvent, stackId));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_STATE")
    public Action<?, ?> migrateZookeeperToKraftAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft state started {}", payload);
                Long stackId = payload.getResourceId();
                String nextEvent = MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftEvent(nextEvent, stackId));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE")
    public Action<?, ?> migrateZookeeperToKraftFinished() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft finished state started {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE);
                flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), CLUSTER_KRAFT_MIGRATION_FINISHED_EVENT);
                String nextEventSelector = FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event();
                sendEvent(context, nextEventSelector, new MigrateZookeeperToKraftEvent(nextEventSelector, stackId));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext, Exception ex) {
                return new MigrateZookeeperToKraftFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_FAILED_STATE")
    public Action<?, ?> migrateZookeeperToKraftFailed() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftFailureEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftFailureEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Migrate Zookeeper to KRaft failed: {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED);
                flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_KRAFT_MIGRATION_FAILED_EVENT, payload.getException().getMessage());
                sendEvent(context, HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftFailureEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftFailureEvent(payload.getResourceId(), ex);
            }
        };
    }
}
