package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.status.AmbariClusterStatusUpdater;
import com.sequenceiq.cloudbreak.service.proxy.ProxyRegistrator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

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
    private StackUtil stackUtil;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterSyncRequest.class);
    }

    @Override
    public void accept(Event<ClusterSyncRequest> event) {
        ClusterSyncRequest request = event.getData();
        ClusterSyncResult result;
        try {
            Stack stack = stackService.getByIdWithLists(request.getStackId());
            Gateway gateway = stack.getCluster().getGateway();
            if (gateway != null && gateway.getEnableGateway()
                    && gateway.getGatewayType() == GatewayType.CENTRAL) {
                String proxyIp = stackUtil.extractAmbariIp(stack);
                proxyRegistrator.register(stack.getName(), gateway.getPath(), proxyIp);
            }
            Cluster cluster = clusterService.retrieveClusterByStackId(request.getStackId());
            ambariClusterStatusUpdater.updateClusterStatus(stack, cluster);
            result = new ClusterSyncResult(request);
        } catch (Exception e) {
            result = new ClusterSyncResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
