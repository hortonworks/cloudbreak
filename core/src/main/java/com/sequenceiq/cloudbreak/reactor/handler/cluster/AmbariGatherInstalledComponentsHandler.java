package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariGatherInstalledComponentsHandler implements ReactorEventHandler<AmbariGatherInstalledComponentsRequest> {

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
        Long stackId = request.getStackId();
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
