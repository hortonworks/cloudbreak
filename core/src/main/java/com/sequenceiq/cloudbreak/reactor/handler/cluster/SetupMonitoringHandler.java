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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SetupMonitoringFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SetupMonitoringRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SetupMonitoringSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class SetupMonitoringHandler implements EventHandler<SetupMonitoringRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupMonitoringHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SetupMonitoringRequest.class);
    }

    @Override
    public void accept(Event<SetupMonitoringRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.setupMonitoring(stackId);
            response = new SetupMonitoringSuccess(stackId);
        } catch (RuntimeException | CloudbreakException e) {
            LOGGER.error("Build cluster failed", e);
            response = new SetupMonitoringFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
