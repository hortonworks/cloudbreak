package com.sequenceiq.datalake.flow.upgrade.database;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_IN_PROGRESS;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerStateSelectors.SDX_UPGRADE_DATABASE_SERVER_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerStateSelectors.SDX_UPGRADE_DATABASE_SERVER_FINALIZED_EVENT;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerSuccessEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerWaitSuccessEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.datalake.flow.upgrade.database.event.UpgradeDatabaseServerWaitRequest;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.upgrade.database.SdxRdsUpgradeValidationErrorHandler;

@Configuration
public class UpgradeDatabaseServerActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDatabaseServerActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Inject
    private SdxRdsUpgradeValidationErrorHandler sdxRdsUpgradeValidationErrorHandler;

    @Bean(name = "SDX_UPGRADE_DATABASE_SERVER_UPGRADE_STATE")
    public Action<?, ?> callUpgradeDatabaseInCore() {
        return new AbstractUpgradeDatabaseServerSdxAction<>(SdxUpgradeDatabaseServerEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SdxUpgradeDatabaseServerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Execute upgrade database server flow for SDX: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_UPGRADE_DATABASE_SERVER_IN_PROGRESS, "Database server upgrade is in progress",
                        payload.getResourceId());
                UpgradeDatabaseServerRequest request = UpgradeDatabaseServerRequest.from(context, payload.getTargetMajorVersion(), payload.isForced());
                sendEvent(context, request);
            }

            @Override
            protected Object getFailurePayload(SdxUpgradeDatabaseServerEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxUpgradeDatabaseServerFailedEvent.from(payload, ex, "Error launching database server upgrade");
            }
        };
    }

    @Bean(name = "SDX_UPGRADE_DATABASE_SERVER_WAIT_UPGRADE_STATE")
    public Action<?, ?> waitForUpgradeDatabaseInCore() {
        return new AbstractUpgradeDatabaseServerSdxAction<>(SdxUpgradeDatabaseServerSuccessEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SdxUpgradeDatabaseServerSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Wait for upgrade database server flow in core for SDX: {}", payload.getResourceId());
                UpgradeDatabaseServerWaitRequest request = UpgradeDatabaseServerWaitRequest.from(context);
                sendEvent(context, request);
            }

        };
    }

    @Bean(name = "SDX_UPGRADE_DATABASE_SERVER_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractUpgradeDatabaseServerSdxAction<>(SdxUpgradeDatabaseServerWaitSuccessEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SdxUpgradeDatabaseServerWaitSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Database server upgrade finalized for SDX: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(
                        DatalakeStatusEnum.RUNNING,
                        ResourceEvent.DATALAKE_UPGRADE_DATABASE_SERVER_FINISHED,
                        "Datalake Database server upgrade completed successfully",
                        payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.UPGRADE_DATABASE_SERVER_FINISHED, sdxCluster);
                sendEvent(context, SDX_UPGRADE_DATABASE_SERVER_FINALIZED_EVENT.event(), payload);
            }

        };
    }

    @Bean(name = "SDX_UPGRADE_DATABASE_SERVER_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractUpgradeDatabaseServerSdxAction<>(SdxUpgradeDatabaseServerFailedEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SdxUpgradeDatabaseServerFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                DatalakeStatusEnum failedStatus = DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", failedStatus, payload.getResourceId(), exception);
                String statusReason = exception.getMessage() != null
                        ? exception.getMessage()
                        : "Database server upgrade failed";
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(
                        failedStatus, Collections.singleton(statusReason), statusReason, payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.UPGRADE_DATABASE_SERVER_FAILED, sdxCluster);
                sdxRdsUpgradeValidationErrorHandler.handleUpgradeValidationError(sdxCluster, exception);
                sendEvent(context, SDX_UPGRADE_DATABASE_SERVER_FAILED_HANDLED_EVENT.event(), payload);
            }

        };
    }
}
