package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.status.AmbariClusterStatusUpdater;
import com.sequenceiq.cloudbreak.service.proxy.ProxyRegistrator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterSyncHandler implements ClusterEventHandler<ClusterSyncRequest> {
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
    private StackUtil stackUtil;

    @Override
    public Class<ClusterSyncRequest> type() {
        return ClusterSyncRequest.class;
    }

    @Override
    public void accept(Event<ClusterSyncRequest> event) {
        ClusterSyncRequest request = event.getData();
        ClusterSyncResult result;
        try {
            Stack stack = stackService.getById(request.getStackId());
            String proxyIp = stackUtil.extractAmbariIp(stack);
            proxyRegistrator.register(stack.getName(), proxyIp);
            Cluster cluster = clusterService.retrieveClusterByStackId(request.getStackId());
            ambariClusterStatusUpdater.updateClusterStatus(stack, cluster);
            result = new ClusterSyncResult(request);
        } catch (Exception e) {
            result = new ClusterSyncResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
