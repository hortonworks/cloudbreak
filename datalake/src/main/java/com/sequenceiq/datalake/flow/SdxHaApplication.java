package com.sequenceiq.datalake.flow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;
import com.sequenceiq.cloudbreak.service.ha.HaApplication;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Primary
@Component
public class SdxHaApplication implements HaApplication {

    public static final String RESOURCE_TYPE = "SDX";

    private static final Set<SdxClusterStatus> DELETE_STATUSES =
            new HashSet<>(Arrays.asList(SdxClusterStatus.DELETE_REQUESTED, SdxClusterStatus.DELETED, SdxClusterStatus.DELETE_FAILED));

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager reactorFlowManager;

    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        return sdxService.findByResourceIdsAndStatuses(resourceIds, DELETE_STATUSES);
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        Set<Long> sdxIds = InMemoryResourceStateStore.getAllResourceId(RESOURCE_TYPE);
        return sdxService.findByResourceIdsAndStatuses(sdxIds, DELETE_STATUSES);
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {
        InMemoryResourceStateStore.deleteResource(RESOURCE_TYPE, resourceId);
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        InMemoryResourceStateStore.putResource(RESOURCE_TYPE, resourceId, PollGroup.CANCELLED);
        reactorFlowManager.cancelRunningFlows(resourceId);
    }
}
