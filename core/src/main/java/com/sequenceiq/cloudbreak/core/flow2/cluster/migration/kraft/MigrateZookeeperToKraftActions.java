package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class MigrateZookeeperToKraftActions {
    private static final Logger LOGGER = getLogger(MigrateZookeeperToKraftActions.class);

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_STATE")
    public Action<?, ?> initMigrateZookeeperToKraftAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftTriggerEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftTriggerEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft init state started {}", payload);
                String nextEvent = MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_EVENT.event();
                sendEvent(context, nextEvent, createMigrateZookeeperToKraftEvent(nextEvent, payload));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftTriggerEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext, Exception ex) {
                return new MigrateZookeeperToKraftFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE")
    public Action<?, ?> validateMigrateZookeeperToKraftAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft validation state started {}", payload);
                String nextEvent = MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event();
                sendEvent(context, nextEvent, createMigrateZookeeperToKraftEvent(nextEvent, payload));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext, Exception ex) {
                return new MigrateZookeeperToKraftFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_STATE")
    public Action<?, ?> upscaleKraftNodesAction() {
        return new AbstractMigrateZookeeperToKraftAction<>(MigrateZookeeperToKraftEvent.class) {

            @Override
            protected MigrateZookeeperToKraftContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    MigrateZookeeperToKraftEvent payload) {
                return MigrateZookeeperToKraftContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(MigrateZookeeperToKraftContext context, MigrateZookeeperToKraftEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Migrate Zookeeper to KRaft upscale KRaft nodes state started {}", payload);
                String nextEvent = MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_EVENT.event();
                sendEvent(context, nextEvent, createMigrateZookeeperToKraftEvent(nextEvent, payload));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext, Exception ex) {
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
                String nextEvent = MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event();
                sendEvent(context, nextEvent, createMigrateZookeeperToKraftEvent(nextEvent, payload));
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext, Exception ex) {
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
                String nextEventSelector = FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event();
                sendEvent(context, nextEventSelector, createMigrateZookeeperToKraftEvent(nextEventSelector, payload));
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
                sendEvent(context, HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(MigrateZookeeperToKraftFailureEvent payload, Optional<MigrateZookeeperToKraftContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    private MigrateZookeeperToKraftEvent createMigrateZookeeperToKraftEvent(String selector, StackEvent payload) {
        return new MigrateZookeeperToKraftEvent(selector, payload.getResourceId());
    }
}