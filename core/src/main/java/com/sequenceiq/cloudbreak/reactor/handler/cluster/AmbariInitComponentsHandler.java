package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInitComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInitComponentsResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariInitComponentsHandler implements EventHandler<ClusterManagerInitComponentsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariInitComponentsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerInitComponentsRequest.class);
    }

    @Override
    public void accept(Event<ClusterManagerInitComponentsRequest> event) {
        ClusterManagerInitComponentsRequest request = event.getData();
        Long stackId = request.getResourceId();
        ClusterManagerInitComponentsResult result;
        try {
            clusterUpscaleService.initComponents(stackId, request.getComponents(), request.getHostName());
            result = new ClusterManagerInitComponentsResult(request);
        } catch (Exception e) {
            String message = "Failed to init components on host";
            LOGGER.error(message, e);
            result = new ClusterManagerInitComponentsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
