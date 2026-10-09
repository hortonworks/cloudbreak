package com.sequenceiq.cloudbreak.service.migration.kraft;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationAction;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.validation.ZookeeperToKraftMigrationValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus;

@Service
public class KraftMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KraftMigrationService.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ZookeeperToKraftMigrationValidator zookeeperToKraftMigrationValidator;

    @Inject
    private KraftMigrationOperationStatusFactory migrationOperationStatusFactory;

    public KraftMigrationStatus getKraftMigrationStatus(StackDto stack) {
        String crn = stack.getResourceCrn();
        LOGGER.info("Getting KRaft migration status for stack with {} CRN", crn);
        boolean kraftMigrationSupported = zookeeperToKraftMigrationValidator.isKraftMigrationStatusSupported(stack, stack.getAccountId());
        return getKraftMigrationStatus(stack, kraftMigrationSupported);
    }

    public KraftMigrationStatusResponse getKraftMigrationStatusResponse(StackDto stack) {
        String crn = stack.getResourceCrn();
        LOGGER.info("Getting KRaft migration status response for stack with {} CRN", crn);
        boolean kraftMigrationSupported = zookeeperToKraftMigrationValidator.isKraftMigrationStatusSupported(stack, stack.getAccountId());
        KraftMigrationOperationStatus migrationOperationStatus = getKraftMigrationOperationStatus(stack, kraftMigrationSupported);

        KraftMigrationAction recommendedAction = getKraftMigrationAction(stack, kraftMigrationSupported, migrationOperationStatus);
        boolean kraftMigrationRequired = recommendedAction == KraftMigrationAction.MIGRATE;
        if (migrationOperationStatus == KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE && !kraftMigrationRequired) {
            migrationOperationStatus = KraftMigrationOperationStatus.NOT_APPLICABLE;
        }
        LOGGER.debug("kraftMigrationSupported: {}, kraftMigrationOperationStatus based on cluster configs: {}, recommendedAction: {}",
                kraftMigrationSupported, migrationOperationStatus, recommendedAction);
        return new KraftMigrationStatusResponse(migrationOperationStatus.name(), recommendedAction.name(), kraftMigrationRequired);
    }

    private KraftMigrationOperationStatus getKraftMigrationOperationStatus(StackDto stack, boolean kraftMigrationSupported) {
        Optional<KraftMigrationOperationStatus> migrationOperationStatusOpt = migrationOperationStatusFactory.getStatusFromFlowInformation(stack);
        KraftMigrationOperationStatus migrationOperationStatus;
        if (migrationOperationStatusOpt.isPresent()) {
            migrationOperationStatus = migrationOperationStatusOpt.get();
        } else {
            LOGGER.debug("There is no Kraft migration flow information, getting info from the cluster.");
            KraftMigrationStatus kraftMigrationStatus = getKraftMigrationStatus(stack, kraftMigrationSupported);
            LOGGER.info("Calculating status based on cluster configs migration status: {}", kraftMigrationStatus);
            migrationOperationStatus = migrationOperationStatusFactory.getStatusFromClusterKRaftMigrationStatus(kraftMigrationStatus);
        }
        return migrationOperationStatus;
    }

    private KraftMigrationStatus getKraftMigrationStatus(StackDto stack, boolean kraftMigrationSupported) {
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.NOT_APPLICABLE;
        boolean clusterAvailable = stack.getStatus().isAvailable();
        if (kraftMigrationSupported && clusterAvailable) {
            kraftMigrationStatus = clusterApiConnectors.getConnector(stack).clusterKraftMigrationStatusService().getKraftMigrationStatus();
            LOGGER.info("Kraft migration status based on the actual Cluster configs: {}", kraftMigrationStatus);
        }
        return kraftMigrationStatus;
    }

    private KraftMigrationAction getKraftMigrationAction(StackDto stack, boolean kraftMigrationSupported,
            KraftMigrationOperationStatus kraftMigrationStatus) {
        if (!kraftMigrationSupported) {
            return KraftMigrationAction.NO_ACTION;
        }

        return switch (kraftMigrationStatus) {
            case ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE ->
                    zookeeperToKraftMigrationValidator.isZookeeperToKRaftMigrationSupportedForRuntimeVersion(stack)
                            ? KraftMigrationAction.MIGRATE : KraftMigrationAction.NO_ACTION;
            case ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE -> KraftMigrationAction.FINALIZE;
            default -> KraftMigrationAction.NO_ACTION;
        };
    }

}
