package com.sequenceiq.externalizedcompute.flow;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.service.ha.HaApplication;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.flow.statestore.ExternalizedComputeInMemoryStateStore;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.service.FlowCancelService;

@Primary
@Component
public class ExternalizedComputeClusterHaApplication implements HaApplication {

    public static final Set<ExternalizedComputeClusterStatusEnum> DELETE_STATUSES =
            Set.of(ExternalizedComputeClusterStatusEnum.DELETED, ExternalizedComputeClusterStatusEnum.DELETE_IN_PROGRESS);

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterHaApplication.class);

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowCancelService flowCancelService;

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        Set<Long> deletingResources = externalizedComputeClusterService.findByResourceIdsAndStatuses(resourceIds, DELETE_STATUSES);
        LOGGER.debug("Resources under deletion from ({}): {}", resourceIds, deletingResources);
        return deletingResources;
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        Set<Long> ids = ExternalizedComputeInMemoryStateStore.getAll();
        LOGGER.debug("All in memory ids: {}", ids);
        Set<Long> deletingResources = externalizedComputeClusterService.findByResourceIdsAndStatuses(ids, DELETE_STATUSES);
        LOGGER.debug("Resurces under deletion from in memory store: {}", deletingResources);
        return deletingResources;
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {
        LOGGER.debug("Cleanup resource from in-memory store: {}", resourceId);
        ExternalizedComputeInMemoryStateStore.delete(resourceId);
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        LOGGER.debug("Cancel running flow for: {}", resourceId);
        ExternalizedComputeInMemoryStateStore.put(resourceId, PollGroup.CANCELLED);
        flowCancelService.cancelRunningFlows(resourceId);
    }

    @Override
    public boolean isRunningOnThisNode(Set<String> runningFlowIds) {
        return runningFlowIds.stream().anyMatch(id -> runningFlows.get(id) != null);
    }
}
