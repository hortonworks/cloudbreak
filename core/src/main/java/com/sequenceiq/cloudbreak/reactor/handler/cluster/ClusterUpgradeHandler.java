package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpgradeService;
import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterUpgradeResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterUpgradeHandler implements ClusterEventHandler<ClusterUpgradeRequest> {
    @Inject
    private AmbariClusterUpgradeService ambariClusterUpgradeService;

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<ClusterUpgradeRequest> type() {
        return ClusterUpgradeRequest.class;
    }

    @Override
    public void accept(Event<ClusterUpgradeRequest> event) {
        ClusterUpgradeRequest request = event.getData();
        ClusterUpgradeResult result;
        try {
            ambariClusterUpgradeService.upgradeCluster(request.getStackId());
            result = new ClusterUpgradeResult(request);
        } catch (Exception e) {
            result = new ClusterUpgradeResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
