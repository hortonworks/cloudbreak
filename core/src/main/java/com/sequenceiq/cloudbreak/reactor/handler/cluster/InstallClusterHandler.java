package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class InstallClusterHandler implements EventHandler<InstallClusterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallClusterHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InstallClusterRequest.class);
    }

    @Override
    public void accept(Event<InstallClusterRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.installCluster(stackId);
            response = new InstallClusterSuccess(stackId);
        } catch (RuntimeException | ClusterClientInitException | CloudbreakException e) {
            LOGGER.error("Failed to Install Cloudera Manager cluster: {}", e.getMessage());
            response = new InstallClusterFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
