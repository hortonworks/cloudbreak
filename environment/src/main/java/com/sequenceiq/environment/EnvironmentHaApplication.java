package com.sequenceiq.environment;

import static com.sequenceiq.environment.environment.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.DELETE_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.DELETE_INITIATED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.service.ha.HaApplication;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.service.FlowCancelService;

@Primary
@Component
public class EnvironmentHaApplication implements HaApplication {

    public static final List<EnvironmentStatus> DELETION_STATUSES
            = List.of(DELETE_INITIATED, NETWORK_DELETE_IN_PROGRESS, FREEIPA_DELETE_IN_PROGRESS, RDBMS_DELETE_IN_PROGRESS, DELETE_FAILED, ARCHIVED);

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentHaApplication.class);

    private final EnvironmentService environmentService;

    private final FlowRegister runningFlows;

    private final FlowCancelService flowCancelService;

    public EnvironmentHaApplication(EnvironmentService environmentService, FlowRegister runningFlows, FlowCancelService flowCancelService) {
        this.environmentService = environmentService;
        this.runningFlows = runningFlows;
        this.flowCancelService = flowCancelService;
    }

    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        return environmentService.findAllIdByIdInAndStatusIn(resourceIds, DELETION_STATUSES);
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        Set<Long> envIds = EnvironmentInMemoryStateStore.getAll();
        return environmentService.findAllIdByIdInAndStatusIn(envIds, DELETION_STATUSES);
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {
        EnvironmentInMemoryStateStore.delete(resourceId);
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        EnvironmentInMemoryStateStore.put(resourceId, PollGroup.CANCELLED);
        flowCancelService.cancelRunningFlows(resourceId);
    }

    @Override
    public boolean isRunningOnThisNode(Set<String> runningFlowIds) {
        return runningFlowIds.stream().anyMatch(id -> runningFlows.get(id) != null);
    }
}
