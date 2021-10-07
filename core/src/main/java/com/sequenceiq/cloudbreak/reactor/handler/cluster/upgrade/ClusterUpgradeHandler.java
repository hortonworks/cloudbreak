package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterUpgradeHandler extends ExceptionCatcherEventHandler<ClusterUpgradeRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ParcelService parcelService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpgradeRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeRequest> event) {
        return new ClusterUpgradeFailedEvent(resourceId, e, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeRequest> event) {
        LOGGER.debug("Accepting Cluster upgrade event..");
        ClusterUpgradeRequest request = event.getData();
        Long stackId = request.getResourceId();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithClusterInTransaction(stackId);
            Optional<String> remoteDataContext = getRemoteDataContext(stack);
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            Set<ClusterComponent> components = parcelService.getParcelComponentsByBlueprint(stack);
            connector.upgradeClusterRuntime(components, request.isPatchUpgrade(), remoteDataContext);

            result = new ClusterUpgradeSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade event failed", e);
            result = new ClusterUpgradeFailedEvent(request.getResourceId(), e, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
        }
        return result;
    }

    private Optional<String> getRemoteDataContext(Stack stack) {
        Optional<String> remoteDataContext = Optional.empty();
        if (!stack.isDatalake() && StringUtils.isNotEmpty(stack.getDatalakeCrn())) {
            Stack datalake = stackService.getByCrn(stack.getDatalakeCrn());
            LOGGER.info("Fetch the Remote Data Context from {} to update the Data Hub", stack.getName());
            ClusterApi datalakeConnector = clusterApiConnectors.getConnector(datalake);
            remoteDataContext = Optional.of(datalakeConnector.getSdxContext());
        }
        return remoteDataContext;
    }
}
