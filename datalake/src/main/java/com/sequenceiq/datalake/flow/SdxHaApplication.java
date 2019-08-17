package com.sequenceiq.datalake.flow;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETE_REQUESTED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.EXTERNAL_DATABASE_DELETION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STACK_DELETED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STACK_DELETION_IN_PROGRESS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.service.ha.HaApplication;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.core.FlowRegister;

@Primary
@Component
public class SdxHaApplication implements HaApplication {

    private static final Set<DatalakeStatusEnum> DELETE_STATUSES =
            new HashSet<>(Arrays.asList(DELETE_REQUESTED,
                    DELETED,
                    STACK_DELETED,
                    STACK_DELETION_IN_PROGRESS,
                    EXTERNAL_DATABASE_DELETION_IN_PROGRESS));

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager reactorFlowManager;

    @Inject
    private FlowRegister runningFlows;

    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        return sdxService.findByResourceIdsAndStatuses(resourceIds, DELETE_STATUSES);
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        Set<Long> sdxIds = DatalakeInMemoryStateStore.getAll();
        return sdxService.findByResourceIdsAndStatuses(sdxIds, DELETE_STATUSES);
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {
        DatalakeInMemoryStateStore.delete(resourceId);
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        DatalakeInMemoryStateStore.put(resourceId, PollGroup.CANCELLED);
        reactorFlowManager.cancelRunningFlows(resourceId);
    }

    @Override
    public boolean isRunningOnThisNode(Set<String> runningFlowIds) {
        return runningFlowIds.stream().anyMatch(id -> runningFlows.get(id) != null);
    }
}
