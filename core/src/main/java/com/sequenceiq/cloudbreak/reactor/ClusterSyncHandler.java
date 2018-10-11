package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.InstanceMetadataUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.status.AmbariClusterStatusUpdater;
import com.sequenceiq.cloudbreak.service.proxy.ProxyRegistrator;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterSyncHandler implements ReactorEventHandler<ClusterSyncRequest> {
    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClusterStatusUpdater ambariClusterStatusUpdater;

    @Inject
    private EventBus eventBus;

    @Inject
    private ProxyRegistrator proxyRegistrator;

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
            Stack stack = stackService.getByIdWithListsInTransaction(request.getStackId());
            proxyRegistrator.registerIfNeed(stack);
            Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(request.getStackId());
            ambariClusterStatusUpdater.updateClusterStatus(stack, cluster);
            if (cluster.isAvailable() || cluster.isMaintenanceModeEnabled()) {
                instanceMetadataUpdater.updatePackageVersionsOnAllInstances(stack);
            }
            result = new ClusterSyncResult(request);
        } catch (Exception e) {
            result = new ClusterSyncResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
