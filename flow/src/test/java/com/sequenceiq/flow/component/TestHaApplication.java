package com.sequenceiq.flow.component;

import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.Commit;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.service.ha.HaApplication;
import com.sequenceiq.flow.service.FlowCancelService;

@Commit
@Primary
public class TestHaApplication implements HaApplication {

    @Inject
    private FlowCancelService flowCancelService;

    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        return Set.of();
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        return Set.of();
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        TestStateStore.put(resourceId, PollGroup.CANCELLED);
        flowCancelService.cancelRunningFlows(resourceId);
    }

    @Override
    public boolean isRunningOnThisNode(Set<String> runningFlowIds) {
        return true;
    }
}
