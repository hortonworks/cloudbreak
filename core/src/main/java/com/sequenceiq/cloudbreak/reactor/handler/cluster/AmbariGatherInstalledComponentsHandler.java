package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class AmbariGatherInstalledComponentsHandler implements EventHandler<AmbariGatherInstalledComponentsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariGatherInstalledComponentsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AmbariGatherInstalledComponentsRequest.class);
    }

    @Override
    public void accept(Event<AmbariGatherInstalledComponentsRequest> event) {
        AmbariGatherInstalledComponentsRequest request = event.getData();
        Long stackId = request.getResourceId();
        AmbariGatherInstalledComponentsResult result;
        try {
            Map<String, String> foundInstalledComponents = clusterUpscaleService.gatherInstalledComponents(stackId, request.getHostName());
            result = new AmbariGatherInstalledComponentsResult(request, foundInstalledComponents);
        } catch (Exception e) {
            String message = "Failed to gather installed components on host";
            LOGGER.error(message, e);
            result = new AmbariGatherInstalledComponentsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
