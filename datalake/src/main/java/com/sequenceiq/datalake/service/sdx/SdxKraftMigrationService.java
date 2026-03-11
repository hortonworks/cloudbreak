package com.sequenceiq.datalake.service.sdx;


import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackKraftMigrationV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.datalake.kraftmigration.KraftMigrationOperationType;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class SdxKraftMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxKraftMigrationService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    public FlowIdentifier migrateFromZookeeperToKraft(String userCrn, String clusterCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxReactorFlowManager.triggerZookeeperToKraftMigrationFlow(sdxCluster);
    }

    public FlowIdentifier finalizeMigrationFromZookeeperToKraft(String userCrn, String clusterCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxReactorFlowManager.triggerZookeeperToKraftFinalizationFlow(sdxCluster);
    }

    public FlowIdentifier rollbackMigrationFromZookeeperToKraft(String userCrn, String clusterCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxReactorFlowManager.triggerZookeeperToKraftRollbackFlow(sdxCluster);
    }

    public KraftMigrationStatusResponse getZookeeperToKraftMigrationStatus(String userCrn, String clusterCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        MDCBuilder.buildMdcContext(sdxCluster);
        LOGGER.debug("Getting ZooKeeper to KRaft migration status for DataLake cluster {}", sdxCluster.getClusterName());
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> cloudbreakInternalCrnClient.withInternalCrn()
                            .stackKraftMigrationV4Endpoint()
                            .zookeeperToKraftMigrationStatusByCrnInternal(sdxCluster.getStackCrn(), userCrn));
        } catch (WebApplicationException e) {
            String message = String.format("Could not retrieve ZooKeeper to KRaft migration status for DataLake cluster %s, reason: %s",
                    sdxCluster.getClusterName(), exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    /**
     * Called from within the DataLake flow (start action) to trigger the appropriate Cloudbreak operation and save the flow ID for polling.
     */
    public void triggerOnCloudbreak(Long sdxId, KraftMigrationOperationType operationType) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        LOGGER.debug("Triggering ZooKeeper to KRaft {} on Cloudbreak for DataLake cluster {}", operationType, sdxCluster.getClusterName());
        sdxStatusService.setStatusForDatalake(getInProgressStatus(operationType), getInProgressMessage(operationType), sdxCluster);
        FlowIdentifier flowIdentifier = callCloudbreakEndpoint(sdxCluster, initiatorUserCrn, operationType);
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
    }

    private DatalakeStatusEnum getInProgressStatus(KraftMigrationOperationType operationType) {
        return switch (operationType) {
            case MIGRATE -> DatalakeStatusEnum.DATALAKE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
            case FINALIZE -> DatalakeStatusEnum.DATALAKE_ZOOKEEPER_TO_KRAFT_FINALIZE_IN_PROGRESS;
            case ROLLBACK -> DatalakeStatusEnum.DATALAKE_ZOOKEEPER_TO_KRAFT_ROLLBACK_IN_PROGRESS;
        };
    }

    private String getInProgressMessage(KraftMigrationOperationType operationType) {
        return switch (operationType) {
            case MIGRATE -> "ZooKeeper to KRaft migration started";
            case FINALIZE -> "ZooKeeper to KRaft migration finalization started";
            case ROLLBACK -> "ZooKeeper to KRaft migration rollback started";
        };
    }

    private FlowIdentifier callCloudbreakEndpoint(SdxCluster sdxCluster, String initiatorUserCrn, KraftMigrationOperationType operationType) {
        String stackCrn = sdxCluster.getStackCrn();
        StackKraftMigrationV4Endpoint endpoint = cloudbreakInternalCrnClient.withInternalCrn().stackKraftMigrationV4Endpoint();
        Function<StackKraftMigrationV4Endpoint, FlowIdentifier> operation = switch (operationType) {
            case MIGRATE   -> e -> e.migrateFromZookeeperToKraftByCrnInternal(stackCrn, initiatorUserCrn);
            case FINALIZE  -> e -> e.finalizeMigrationFromZookeeperToKraftByCrnInternal(stackCrn, initiatorUserCrn);
            case ROLLBACK  -> e -> e.rollbackMigrationFromZookeeperToKraftByCrnInternal(stackCrn, initiatorUserCrn);
        };
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> operation.apply(endpoint));
        } catch (WebApplicationException e) {
            String message = String.format("Could not trigger ZooKeeper to KRaft %s for DataLake cluster %s, reason: %s",
                    operationType, sdxCluster.getClusterName(), exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
