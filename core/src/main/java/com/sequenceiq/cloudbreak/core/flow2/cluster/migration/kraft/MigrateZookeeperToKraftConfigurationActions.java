package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_COMPLETE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_STARTED_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationTriggerEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class MigrateZookeeperToKraftConfigurationActions {
    private static final Logger LOGGER = getLogger(MigrateZookeeperToKraftConfigurationActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE")
    public Action<?, ?> migrateZookeeperToKraftConfigurationAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftConfigurationTriggerEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftConfigurationTriggerEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftConfigurationTriggerEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft configuration state started {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_IN_PROGRESS);
                flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_KRAFT_MIGRATION_STARTED_EVENT);
                String nextEvent = MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftConfigurationEvent(nextEvent, payload.getResourceId()));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftConfigurationTriggerEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftConfigurationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE")
    public Action<?, ?> migrateZookeeperToKraftConfigurationFinished() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftConfigurationEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftConfigurationEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftConfigurationEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft configuration finished state started {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_COMPLETE);
                String nextEvent = FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftConfigurationEvent(nextEvent, payload.getResourceId()));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftConfigurationEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftConfigurationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FAILED_STATE")
    public Action<?, ?> migrateZookeeperToKraftConfigurationFailed() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftConfigurationFailureEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftConfigurationFailureEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftConfigurationFailureEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.error("Migrate Zookeeper to KRaft configuration failed: {}", payload);
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_COMPLETE);
                flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_KRAFT_MIGRATION_FAILED_EVENT);
                sendEvent(context, HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftConfigurationFailureEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return null;
            }
        };
    }
}
