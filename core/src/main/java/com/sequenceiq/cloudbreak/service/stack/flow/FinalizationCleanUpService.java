package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponentHistory;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.structuredevent.db.LegacyStructuredEventDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FinalizationCleanUpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinalizationCleanUpService.class);

    private final LegacyStructuredEventDBService structuredEventDBService;

    private final ClusterComponentConfigProvider clusterComponentConfigProvider;

    private final ClusterService clusterService;

    public FinalizationCleanUpService(LegacyStructuredEventDBService structuredEventDBService,
            ClusterComponentConfigProvider clusterComponentConfigProvider, ClusterService clusterService) {
        this.clusterService = clusterService;
        this.structuredEventDBService = structuredEventDBService;
        this.clusterComponentConfigProvider = clusterComponentConfigProvider;
    }

    public void detachClusterComponentRelatedAuditEntries() throws CleanUpException {
        LOGGER.debug("About to start fetch and clean up detached/orphaned cluster component audit events.");
        executeCleanUp(() -> clusterComponentConfigProvider.cleanUpDetachedEntries(clusterService.findIdsByDeletedStatus()), ClusterComponentHistory.class);
    }

    public void cleanUpStructuredEventsForStack(Long stackId) {
        LOGGER.debug("About to start fetch and clean up structured events.");
        executeCleanUp(() -> structuredEventDBService.deleteEntriesByResourceIdsOlderThanThreeMonths(stackId), StructuredEventEntity.class,
                "that belogs to the following resource ID:" + stackId);
    }

    public void cleanUpStructuredEventsForAccount(String accountId) {
        structuredEventDBService.deleteEntriesForAccountThatIsOlderThanThreeMonths(accountId);
    }

    private void executeCleanUp(Runnable runnable, Class<?> targetEntity) {
        executeCleanUp(runnable, targetEntity, null);
    }

    private void executeCleanUp(Runnable runnable, Class<?> targetEntity, String additionalStartOperationMessage) {
        boolean success = true;
        try {
            LOGGER.debug("About to request the deletion for {}(s){}", targetEntity.getSimpleName(),
                    additionalStartOperationMessage != null ? ' ' + additionalStartOperationMessage : "");
            runnable.run();
        } catch (Exception e) {
            LOGGER.info("Unable to clean up " + targetEntity.getSimpleName() + " entries due to: " + e.getMessage(), e);
            success = false;
            throw new CleanUpException(targetEntity.getSimpleName() + " cleanup has failed!", e);
        } finally {
            String postfix = success ? "successfully." : "with error.";
            LOGGER.debug("Fetching and cleaning up detached/orphaned {} has finished {}", targetEntity.getSimpleName(), postfix);
        }
    }

}
