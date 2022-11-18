package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerUpgradeManagementService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterManagerUpgradeHandler extends ExceptionCatcherEventHandler<ClusterManagerUpgradeRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpgradeHandler.class);

    @Inject
    private ClusterManagerUpgradeManagementService clusterManagerUpgradeManagementService;

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterManagerUpgradeRequest> event) {
        return new ClusterUpgradeFailedEvent(resourceId, e, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterManagerUpgradeRequest> event) {
        LOGGER.debug("Accepting Cluster Manager upgrade event..");
        ClusterManagerUpgradeRequest request = event.getData();
        try {
            clusterManagerUpgradeManagementService.upgradeClusterManager(request.getResourceId(), request.isRuntimeServicesStartNeeded(),
                    request.isRollingUpgradeEnabled());
            return new ClusterManagerUpgradeSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.info("Cluster Manager upgrade event failed", e);
            return new ClusterUpgradeFailedEvent(request.getResourceId(), e, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED);
        }
    }
}
