package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class ClusterUpgradeHandler extends ExceptionCatcherEventHandler<ClusterUpgradeRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpgradeRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new ClusterUpgradeFailedEvent(resourceId, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        LOGGER.debug("Accepting Cluster upgrade event..");
        ClusterUpgradeRequest request = event.getData();
        Long stackId = request.getResourceId();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithClusterInTransaction(stackId);
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            Set<ClusterComponent> components = clusterComponentProvider.getComponentsByClusterId(stack.getCluster().getId());
            connector.upgradeClusterRuntime(components);

            result = new ClusterUpgradeSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.info("Cluster upgrade event failed", e);
            result = new ClusterUpgradeFailedEvent(request.getResourceId(), e);
        }
        sendEvent(result, event);
    }
}
