package com.sequenceiq.datalake.flow.sku;

import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowEvent.DATALAKE_SKU_MIGRATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowEvent.DATALAKE_SKU_MIGRATION_FINALIZED_EVENT;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.sku.handler.WaitDataLakeSkuMigrationRequest;
import com.sequenceiq.datalake.flow.sku.handler.WaitDataLakeSkuMigrationResult;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class DataLakeSkuMigrationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeSkuMigrationActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Inject
    private CloudbreakStackService cloudbreakStackService;

    @Bean(name = "DATALAKE_SKU_MIGRATION_STATE")
    public Action<?, ?> migrateDatalakeSkus() {

        return new AbstractSdxAction<>(DataLakeSkuMigrationTriggerEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DataLakeSkuMigrationTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Initiating the process to migrate Sku to STANDARD for the Data Lake. Force: {}", payload.isForce());
                WaitDataLakeSkuMigrationRequest waitDataLakeSkuMigrationRequest = new WaitDataLakeSkuMigrationRequest(payload.getResourceId(),
                        context.getUserId(), payload.isForce());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SKU_MIGRATION_CHANGE_IN_PROGRESS,
                        "Initiating the process to migrate Skus for the Data Lake",
                        context.getSdxId());
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                cloudbreakStackService.migrateDatalakeSkus(sdxCluster, payload.isForce());
                LOGGER.info("Successfully initiated the process to migrate Skus for the Data Lake with payload: {}", payload);
                sendEvent(context, waitDataLakeSkuMigrationRequest);
            }

            @Override
            protected Object getFailurePayload(DataLakeSkuMigrationTriggerEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DataLakeSkuMigrationFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATALAKE_SKU_MIGRATION_FINISED_STATE")
    public Action<?, ?> migrateDatalakeSkusFinished() {

        return new AbstractSdxAction<>(WaitDataLakeSkuMigrationResult.class) {
            @Override
            protected void doExecute(SdxContext context, WaitDataLakeSkuMigrationResult payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Successfully migrated Skus for the Data Lake");
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        "Successfully migrated Skus for the Data Lake", context.getSdxId());
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                Set<ProviderSyncState> providerSyncStates = sdxCluster.getProviderSyncStates();
                LOGGER.info("Removing BASIC_SKU_MIGRATION_NEEDED from provider sync states for datalake: {}", sdxCluster.getClusterName());
                providerSyncStates.remove(ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED);
                sdxService.save(sdxCluster);
                sendEvent(context, new SdxEvent(DATALAKE_SKU_MIGRATION_FINALIZED_EVENT.event(), context.getSdxId(), context.getUserId()));
            }

            @Override
            protected Object getFailurePayload(WaitDataLakeSkuMigrationResult payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DataLakeSkuMigrationFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATALAKE_SKU_MIGRATION_FAILED_STATE")
    public Action<?, ?> migrateDatalakeSkusFailed() {
        return new AbstractSdxAction<>(DataLakeSkuMigrationFailedEvent.class) {

            @Override
            protected void doExecute(SdxContext context, DataLakeSkuMigrationFailedEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.warn("Failed to migrate Skus for datalake");
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SKU_MIGRATION_CHANGE_FAILED,
                        Collections.singleton(payload.getException().getMessage()), "Failed to migrate Skus for datalake",
                        context.getSdxId());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(DataLakeSkuMigrationFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new SdxEvent(DATALAKE_SKU_MIGRATION_FAIL_HANDLED_EVENT.event(), context.getSdxId(), context.getUserId());
            }
        };
    }

}
