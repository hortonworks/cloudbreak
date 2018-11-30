package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopComponentsResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariStopComponentsHandler implements ReactorEventHandler<AmbariStopComponentsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStopComponentsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AmbariStopComponentsRequest.class);
    }

    @Override
    public void accept(Event<AmbariStopComponentsRequest> event) {
        AmbariStopComponentsRequest request = event.getData();
        Long stackId = request.getStackId();
        AmbariStopComponentsResult result;
        try {
            clusterUpscaleService.stopComponents(stackId, request.getComponents(), request.getHostName());
            result = new AmbariStopComponentsResult(request);
        } catch (Exception e) {
            String message = "Failed to install components on new host";
            LOGGER.error(message, e);
            result = new AmbariStopComponentsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));

    }

}
