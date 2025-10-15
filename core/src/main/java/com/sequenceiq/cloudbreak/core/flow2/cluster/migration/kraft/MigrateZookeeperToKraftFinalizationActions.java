package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationHandlerSelectors.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FINALIZE_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.HANDLED_FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FINALIZATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FINALIZATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FINALIZATION_STARTED_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationTriggerEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class MigrateZookeeperToKraftFinalizationActions {
    private static final Logger LOGGER = getLogger(MigrateZookeeperToKraftFinalizationActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Bean(name = "FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_STATE")
    public Action<?, ?> finalizeZookeeperToKraftMigrationAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftFinalizationTriggerEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftFinalizationTriggerEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftFinalizationTriggerEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.debug("Finalize Zookeeper to KRaft migration state started {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS);
                flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_KRAFT_MIGRATION_FINALIZATION_STARTED_EVENT);
                String nextEvent = FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftFinalizationEvent(nextEvent, payload.getResourceId()));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftFinalizationTriggerEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftFinalizationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE")
    public Action<?, ?> finalizeZookeeperToKraftMigrationFinished() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftFinalizationEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftFinalizationEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftFinalizationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Finalize Zookeeper to KRaft migration finished state started {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE);
                flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_KRAFT_MIGRATION_FINALIZATION_FINISHED_EVENT);
                String nextEvent = FINALIZE_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftFinalizationEvent(nextEvent, payload.getResourceId()));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftFinalizationEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftFinalizationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED_STATE")
    public Action<?, ?> finalizeZookeeperToKraftMigrationFailed() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftFinalizationFailureEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftFinalizationFailureEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftFinalizationFailureEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.error("Finalize Zookeeper to KRaft migration failed: {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED);
                flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_KRAFT_MIGRATION_FINALIZATION_FAILED_EVENT);
                sendEvent(context, HANDLED_FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftFinalizationFailureEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return null;
            }
        };
    }
}