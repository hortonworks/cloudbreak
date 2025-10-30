package com.sequenceiq.cloudbreak.service.migration.kraft;

import java.util.Comparator;
import java.util.List;
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
import com.sequenceiq.flow.api.model.FlowLogResponse;

@Service
public class KraftMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KraftMigrationService.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ZookeeperToKraftMigrationValidator zookeeperToKraftMigrationValidator;

    public KraftMigrationStatusResponse getKraftMigrationStatus(StackDto stack, List<FlowLogResponse> kraftFlowLogResponseList) {
        LOGGER.info("Getting KRaft migration status for stack with {} CRN", stack.getResourceCrn());
        boolean kraftMigrationSupported = zookeeperToKraftMigrationValidator.isMigrationFromZookeeperToKraftSupported(stack, stack.getAccountId());
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.NOT_APPLICABLE;
        if (kraftMigrationSupported) {
            kraftMigrationStatus = clusterApiConnectors.getConnector(stack).clusterKraftMigrationStatusService().getKraftMigrationStatus();
        }
        KraftMigrationAction recommendedAction = getKraftMigrationAction(kraftMigrationSupported, kraftMigrationStatus);
        boolean kraftMigrationRequired = isKraftMigrationRequired(kraftMigrationSupported, kraftMigrationStatus);
        String mostRecentKraftFlowId = getMostRecentKraftFlowId(kraftFlowLogResponseList).orElse(null);
        LOGGER.debug("kraftMigrationSupported: {}, kraftMigrationStatus: {}, recommendedAction: {}, mostRecentKraftFlowId: {}", kraftMigrationSupported,
                kraftMigrationStatus, recommendedAction, mostRecentKraftFlowId);
        return new KraftMigrationStatusResponse(kraftMigrationStatus.name(), recommendedAction.name(), kraftMigrationRequired,
                mostRecentKraftFlowId);
    }

    private Optional<String> getMostRecentKraftFlowId(List<FlowLogResponse> kraftFlowLogResponseList) {
        Optional<String> mostRecentKraftFlowId = kraftFlowLogResponseList.stream()
                .max(Comparator.comparingLong(FlowLogResponse::getCreated))
                .map(FlowLogResponse::getFlowId);
        return mostRecentKraftFlowId;
    }

    private boolean isKraftMigrationRequired(boolean kraftMigrationSupported, KraftMigrationStatus kraftMigrationStatus) {
        return kraftMigrationSupported && KraftMigrationStatus.ZOOKEEPER_INSTALLED.equals(kraftMigrationStatus);
    }

    private KraftMigrationAction getKraftMigrationAction(boolean kraftMigrationSupported, KraftMigrationStatus kraftMigrationStatus) {
        if (!kraftMigrationSupported) {
            return KraftMigrationAction.NO_ACTION;
        }

        return switch (kraftMigrationStatus) {
            case ZOOKEEPER_INSTALLED -> KraftMigrationAction.MIGRATE;
            case BROKERS_IN_KRAFT -> KraftMigrationAction.FINALIZE;
            default -> KraftMigrationAction.NO_ACTION;
        };
    }
}
