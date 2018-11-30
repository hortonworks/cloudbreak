package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariInitComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariInitComponentsResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariInitComponentsHandler implements ReactorEventHandler<AmbariInitComponentsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariInitComponentsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AmbariInitComponentsRequest.class);
    }

    @Override
    public void accept(Event<AmbariInitComponentsRequest> event) {
        AmbariInitComponentsRequest request = event.getData();
        Long stackId = request.getStackId();
        AmbariInitComponentsResult result;
        try {
            clusterUpscaleService.initComponents(stackId, request.getComponents(), request.getHostName());
            result = new AmbariInitComponentsResult(request);
        } catch (Exception e) {
            String message = "Failed to init components on host";
            LOGGER.error(message, e);
            result = new AmbariInitComponentsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
