package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStartComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStartComponentsResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariStartComponentsHandler implements ReactorEventHandler<AmbariStartComponentsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStartComponentsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AmbariStartComponentsRequest.class);
    }

    @Override
    public void accept(Event<AmbariStartComponentsRequest> event) {
        AmbariStartComponentsRequest request = event.getData();
        Long stackId = request.getStackId();
        AmbariStartComponentsResult result;
        try {
            clusterUpscaleService.startComponents(stackId, request.getComponents(), request.getHostName());
            result = new AmbariStartComponentsResult(request);
        } catch (Exception e) {
            String message = "Failed to start components on new host";
            LOGGER.error(message, e);
            result = new AmbariStartComponentsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));

    }
}
