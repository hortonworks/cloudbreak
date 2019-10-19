package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRestartAllRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRestartAllResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariRestartAllHandler implements EventHandler<AmbariRestartAllRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRestartAllHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AmbariRestartAllRequest.class);
    }

    @Override
    public void accept(Event<AmbariRestartAllRequest> event) {
        AmbariRestartAllRequest request = event.getData();
        Long stackId = request.getResourceId();
        AmbariRestartAllResult result;
        try {
            clusterUpscaleService.restartAll(stackId);
            result = new AmbariRestartAllResult(request);
        } catch (Exception e) {
            String message = "Failed to restart all";
            LOGGER.error(message, e);
            result = new AmbariRestartAllResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

}
