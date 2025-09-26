package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftValidationTriggerEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class MigrateZookeeperToKraftValidationActions {
    private static final Logger LOGGER = getLogger(MigrateZookeeperToKraftValidationActions.class);

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE")
    public Action<?, ?> migrateZookeeperToKraftValidationAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftValidationTriggerEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftValidationTriggerEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftValidationTriggerEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft init state started {}", payload);
                String nextEvent = MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event();
                sendEvent(context, nextEvent, new MigrateZookeeperToKraftValidationEvent(nextEvent, payload.getResourceId()));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftValidationTriggerEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftValidationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_FINISHED_STATE")
    public Action<?, ?> migrateZookeeperToKraftValidationFinished() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftValidationEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftValidationEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftValidationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft finished state started {}", payload);
                String nextEventSelector = FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event();
                sendEvent(context, nextEventSelector, new MigrateZookeeperToKraftValidationEvent(nextEventSelector, payload.getResourceId()));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftValidationEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return new MigrateZookeeperToKraftValidationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_FAILED_STATE")
    public Action<?, ?> migrateZookeeperToKraftValidationFailed() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftValidationFailureEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftValidationFailureEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftValidationFailureEvent payload,
                    Map<Object, Object> variables) {
                LOGGER.error("Migrate Zookeeper to KRaft failed: {}", payload);
                sendEvent(context, HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftValidationFailureEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext,
                    Exception ex) {
                return null;
            }
        };
    }
}