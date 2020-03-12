package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerUpgradeService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.EventBus;

@Component
public class ClusterManagerUpgradeHandler extends ExceptionCatcherEventHandler<ClusterManagerUpgradeRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpgradeHandler.class);

    @Inject
    private ClusterManagerUpgradeService clusterManagerUpgradeService;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new ClusterUpgradeFailedEvent(resourceId, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        LOGGER.debug("Accepting Cluster Manager upgrade event..");
        ClusterManagerUpgradeRequest request = event.getData();
        Selectable result;
        try {
            clusterManagerUpgradeService.upgradeClusterManager(request.getResourceId());
            result = new ClusterManagerUpgradeSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.info("Cluster Manager upgrade event failed", e);
            result = new ClusterUpgradeFailedEvent(request.getResourceId(), e);
        }
        sendEvent(result, event);
    }
}
