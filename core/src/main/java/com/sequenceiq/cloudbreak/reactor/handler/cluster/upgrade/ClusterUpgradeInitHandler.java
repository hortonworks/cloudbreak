package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterUpgradeInitHandler extends ExceptionCatcherEventHandler<ClusterUpgradeInitRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeInitHandler.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private ParcelService parcelService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpgradeInitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeInitRequest> event) {
        return new ClusterUpgradeFailedEvent(resourceId, e, DetailedStackStatus.CLUSTER_UPGRADE_INIT_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeInitRequest> event) {
        LOGGER.debug("Accepting Cluster Manager parcel deactivation event..");
        Stack stack = stackService.getByIdWithClusterInTransaction(event.getData().getResourceId());
        ClusterUpgradeInitRequest request = event.getData();
        Selectable result;
        try {
            Set<ClusterComponent> componentsByBlueprint = parcelService.getParcelComponentsByBlueprint(stack);
            parcelService.removeUnusedParcelComponents(stack, componentsByBlueprint);
            clusterApiConnectors.getConnector(stack).downloadAndDistributeParcels(componentsByBlueprint, request.isPatchUpgrade());
            result = new ClusterUpgradeInitSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Cluster Manager parcel deactivation failed", e);
            result = new ClusterUpgradeFailedEvent(request.getResourceId(), e, DetailedStackStatus.CLUSTER_UPGRADE_INIT_FAILED);
        }
        return result;
    }
}
