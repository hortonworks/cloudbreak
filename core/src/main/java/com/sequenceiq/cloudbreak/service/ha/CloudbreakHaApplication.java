package com.sequenceiq.cloudbreak.service.ha;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.service.FlowCancelService;

@Primary
@Component
public class CloudbreakHaApplication implements HaApplication {
    private static final List<Status> DELETE_STATUSES = Arrays.asList(Status.DELETE_IN_PROGRESS, Status.DELETE_COMPLETED, Status.DELETE_FAILED);

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowCancelService flowCancelService;

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
        StackView stack = stackDtoService.getStackViewById(resourceId);
        InMemoryStateStore.deleteStack(resourceId);
        if (stack.getClusterId() != null) {
            InMemoryStateStore.deleteCluster(stack.getClusterId());
        }
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        InMemoryStateStore.putStack(resourceId, PollGroup.CANCELLED);
        flowCancelService.cancelRunningFlows(resourceId);
    }

    @Override
    public boolean isRunningOnThisNode(Set<String> runningFlowIds) {
        return runningFlowIds.stream().anyMatch(id -> runningFlows.getFlowChainId(id) != null);
    }
}
