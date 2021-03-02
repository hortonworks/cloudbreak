package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.HandleClusterCreationSuccessFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.HandleClusterCreationSuccessRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.HandleClusterCreationSuccessSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterInstallSuccessHandler implements EventHandler<HandleClusterCreationSuccessRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterInstallSuccessHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(HandleClusterCreationSuccessRequest.class);
    }

    @Override
    public void accept(Event<HandleClusterCreationSuccessRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.handleClusterCreationSuccess(stackId);
            response = new HandleClusterCreationSuccessSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("Build cluster failed", e);
            response = new HandleClusterCreationSuccessFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
