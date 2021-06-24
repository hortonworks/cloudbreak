package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AutoConfigureClusterManagerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AutoConfigureClusterManagerFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AutoConfigureClusterManagerSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AutoConfigureClusterManagerHandler implements EventHandler<AutoConfigureClusterManagerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoConfigureClusterManagerHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AutoConfigureClusterManagerRequest.class);
    }

    @Override
    public void accept(Event<AutoConfigureClusterManagerRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.autoConfigureCluster(stackId);
            response = new AutoConfigureClusterManagerSuccess(stackId);
        } catch (RuntimeException | ClusterClientInitException | CloudbreakException e) {
            LOGGER.error("Failed to autoconfigure Cloudera Manager cluster: {}", e.getMessage());
            response = new AutoConfigureClusterManagerFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
