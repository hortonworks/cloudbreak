package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.InstanceMetadataUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.status.ClusterStatusUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterSyncHandler implements EventHandler<ClusterSyncRequest> {
    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterStatusUpdater clusterStatusUpdater;

    @Inject
    private EventBus eventBus;

    @Inject
    private InstanceMetadataUpdater instanceMetadataUpdater;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterSyncRequest.class);
    }

    @Override
    public void accept(Event<ClusterSyncRequest> event) {
        ClusterSyncRequest request = event.getData();
        ClusterSyncResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(request.getResourceId()).orElse(null);
            clusterStatusUpdater.updateClusterStatus(stack, cluster);
            if (cluster != null && (cluster.isAvailable() || cluster.isMaintenanceModeEnabled())) {
                instanceMetadataUpdater.updatePackageVersionsOnAllInstances(stack.getId());
            }
            result = new ClusterSyncResult(request);
        } catch (Exception e) {
            result = new ClusterSyncResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

}
