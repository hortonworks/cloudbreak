package com.sequenceiq.freeipa.flow;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.service.ha.HaApplication;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.dto.StackIdWithStatus;
import com.sequenceiq.freeipa.service.stack.StackService;

@Primary
@Component
public class FreeIpaHaApplication implements HaApplication {
    private static final List<Status> DELETE_STATUSES = Arrays.asList(Status.DELETE_IN_PROGRESS, Status.DELETE_COMPLETED, Status.DELETE_FAILED);

    @Inject
    private StackService stackService;

    @Inject
    private FlowCancelService flowCancelService;

    @Inject
    private FlowRegister runningFlows;

    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        return stackService.getStatuses(resourceIds).stream()
                .filter(ss -> DELETE_STATUSES.contains(ss.getStatus())).map(StackIdWithStatus::getId).collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        Set<Long> stackIds = InMemoryStateStore.getAllStackId();
        return stackIds.isEmpty() ? Set.of() : stackService.getStatuses(stackIds).stream()
            .filter(ss -> DELETE_STATUSES.contains(ss.getStatus())).map(StackIdWithStatus::getId).collect(Collectors.toSet());
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {
        InMemoryStateStore.deleteStack(resourceId);
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        InMemoryStateStore.putStack(resourceId, PollGroup.CANCELLED);
        flowCancelService.cancelRunningFlows(resourceId);
    }

    @Override
    public boolean isRunningOnThisNode(Set<String> runningFlowIds) {
        return runningFlowIds.stream().anyMatch(id -> runningFlows.get(id) != null);
    }
}
