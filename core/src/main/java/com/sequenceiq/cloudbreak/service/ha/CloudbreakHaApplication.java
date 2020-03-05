package com.sequenceiq.cloudbreak.service.ha;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowRegister;

@Primary
@Component
public class CloudbreakHaApplication implements HaApplication {
    private static final List<Status> DELETE_STATUSES = Arrays.asList(Status.DELETE_IN_PROGRESS, Status.DELETE_COMPLETED, Status.DELETE_FAILED);

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    private FlowRegister runningFlows;

    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        return stackService.getStatuses(resourceIds).stream()
                .filter(ss -> DELETE_STATUSES.contains(ss.getStatus().getStatus())).map(StackStatusView::getId).collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        Set<Long> stackIds = InMemoryStateStore.getAllStackId();
        return stackIds.isEmpty() ? Set.of() : stackService.getStatuses(stackIds).stream()
            .filter(ss -> DELETE_STATUSES.contains(ss.getStatus().getStatus())).map(StackStatusView::getId).collect(Collectors.toSet());
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {
        Stack stack = stackService.getByIdWithTransaction(resourceId);
        InMemoryStateStore.deleteStack(resourceId);
        if (stack.getCluster() != null) {
            InMemoryStateStore.deleteCluster(stack.getCluster().getId());
        }
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        InMemoryStateStore.putStack(resourceId, PollGroup.CANCELLED);
        reactorFlowManager.cancelRunningFlows(resourceId);
    }

    @Override
    public boolean isRunningOnThisNode(Set<String> runningFlowIds) {
        return runningFlowIds.stream().anyMatch(id -> runningFlows.getFlowChainId(id) != null);
    }
}
