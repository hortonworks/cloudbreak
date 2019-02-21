package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerUpgradeService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterUpgradeResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterUpgradeHandler implements ReactorEventHandler<ClusterUpgradeRequest> {
    @Inject
    private ClusterManagerUpgradeService clusterManagerUpgradeService;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpgradeRequest.class);
    }

    @Override
    public void accept(Event<ClusterUpgradeRequest> event) {
        ClusterUpgradeRequest request = event.getData();
        ClusterUpgradeResult result;
        try {
            clusterManagerUpgradeService.upgradeCluster(request.getStackId());
            result = new ClusterUpgradeResult(request);
        } catch (Exception e) {
            result = new ClusterUpgradeResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
