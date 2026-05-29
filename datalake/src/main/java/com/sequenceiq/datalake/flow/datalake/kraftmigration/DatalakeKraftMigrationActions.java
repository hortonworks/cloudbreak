package com.sequenceiq.datalake.flow.datalake.kraftmigration;

import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.kraftmigration.event.DatalakeKraftMigrationFailedEvent;
import com.sequenceiq.datalake.flow.datalake.kraftmigration.event.DatalakeKraftMigrationStartEvent;
import com.sequenceiq.datalake.flow.datalake.kraftmigration.event.DatalakeKraftMigrationWaitRequest;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxKraftMigrationService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatalakeKraftMigrationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeKraftMigrationActions.class);

    private static final String OPERATION_TYPE_KEY = "operationType";

    @Bean(name = "DATALAKE_KRAFT_MIGRATION_START_STATE")
    public Action<?, ?> kraftMigrationStartAction() {
        return new AbstractSdxAction<>(DatalakeKraftMigrationStartEvent.class) {

            @Inject
            private SdxKraftMigrationService sdxKraftMigrationService;

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeKraftMigrationStartEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeKraftMigrationStartEvent payload, Map<Object, Object> variables) {
                variables.put(OPERATION_TYPE_KEY, payload.getOperationType());
                LOGGER.info("ZooKeeper to KRaft {} started for DataLake id {}", payload.getOperationType(), payload.getResourceId());
                sdxKraftMigrationService.triggerOnCloudbreak(payload.getResourceId(), payload.getOperationType());
                sendEvent(context, new SdxEvent(DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_EVENT.event(), context));
            }

            @Override
            protected Object getFailurePayload(DatalakeKraftMigrationStartEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("ZooKeeper to KRaft {} failed to start for DataLake id {}", payload.getOperationType(), payload.getResourceId(), ex);
                return DatalakeKraftMigrationFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_STATE")
    public Action<?, ?> kraftMigrationInProgressAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("ZooKeeper to KRaft migration is in progress for DataLake id {}", payload.getResourceId());
                sendEvent(context, DatalakeKraftMigrationWaitRequest.from(context));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("ZooKeeper to KRaft migration in-progress state failed for DataLake id {}", payload.getResourceId(), ex);
                return DatalakeKraftMigrationFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "DATALAKE_KRAFT_MIGRATION_FINISHED_STATE")
    public Action<?, ?> kraftMigrationFinishedAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Inject
            private SdxStatusService sdxStatusService;

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                KraftMigrationOperationType operationType = (KraftMigrationOperationType) variables.get(OPERATION_TYPE_KEY);
                LOGGER.info("ZooKeeper to KRaft {} finished for DataLake id {}", operationType, payload.getResourceId());
                sdxStatusService.setStatusForDatalake(
                        DatalakeStatusEnum.RUNNING,
                        getFinishedMessage(operationType),
                        payload.getResourceId());
                sendEvent(context, DATALAKE_KRAFT_MIGRATION_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("ZooKeeper to KRaft migration finalization failed for DataLake id {}", payload.getResourceId(), ex);
                return DatalakeKraftMigrationFailedEvent.from(payload, ex);
            }

            private String getFinishedMessage(KraftMigrationOperationType operationType) {
                if (operationType == null) {
                    return "ZooKeeper to KRaft migration finished";
                }
                return switch (operationType) {
                    case MIGRATE -> "ZooKeeper to KRaft migration finished";
                    case FINALIZE -> "ZooKeeper to KRaft migration finalization finished";
                    case ROLLBACK -> "ZooKeeper to KRaft migration rollback finished";
                };
            }
        };
    }

    @Bean(name = "DATALAKE_KRAFT_MIGRATION_FAILED_STATE")
    public Action<?, ?> kraftMigrationFailedAction() {
        return new AbstractSdxAction<>(DatalakeKraftMigrationFailedEvent.class) {

            @Inject
            private SdxStatusService sdxStatusService;

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeKraftMigrationFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeKraftMigrationFailedEvent payload, Map<Object, Object> variables) {
                KraftMigrationOperationType operationType = (KraftMigrationOperationType) variables.get(OPERATION_TYPE_KEY);
                LOGGER.error("ZooKeeper to KRaft {} failed for DataLake id {}", operationType, payload.getResourceId());
                sdxStatusService.setStatusForDatalake(
                        getFailedStatusEnum(operationType),
                        "ZooKeeper to KRaft migration failed: " + payload.getException().getMessage(),
                        payload.getResourceId());
                sendEvent(context, DATALAKE_KRAFT_MIGRATION_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(DatalakeKraftMigrationFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                LOGGER.error("ZooKeeper to KRaft migration failure handling failed unexpectedly!", ex);
                return null;
            }

            private DatalakeStatusEnum getFailedStatusEnum(KraftMigrationOperationType operationType) {
                if (operationType == null) {
                    return DatalakeStatusEnum.DATALAKE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
                }
                return switch (operationType) {
                    case MIGRATE -> DatalakeStatusEnum.DATALAKE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
                    case FINALIZE -> DatalakeStatusEnum.DATALAKE_ZOOKEEPER_TO_KRAFT_FINALIZE_FAILED;
                    case ROLLBACK -> DatalakeStatusEnum.DATALAKE_ZOOKEEPER_TO_KRAFT_ROLLBACK_FAILED;
                };
            }
        };
    }
}
