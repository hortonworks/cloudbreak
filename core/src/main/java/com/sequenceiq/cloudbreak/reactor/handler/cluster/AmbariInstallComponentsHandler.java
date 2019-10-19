package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInstallComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInstallComponentsResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariInstallComponentsHandler implements EventHandler<ClusterManagerInstallComponentsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariInstallComponentsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerInstallComponentsRequest.class);
    }

    @Override
    public void accept(Event<ClusterManagerInstallComponentsRequest> event) {
        ClusterManagerInstallComponentsRequest request = event.getData();
        Long stackId = request.getResourceId();
        ClusterManagerInstallComponentsResult result;
        try {
            clusterUpscaleService.installComponents(stackId, request.getComponents(), request.getHostName());
            result = new ClusterManagerInstallComponentsResult(request);
        } catch (Exception e) {
            String message = "Failed to install components on new host";
            LOGGER.error(message, e);
            result = new ClusterManagerInstallComponentsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));

    }
}
