package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariInstallComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariInstallComponentsResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariInstallComponentsHandler implements ReactorEventHandler<AmbariInstallComponentsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariInstallComponentsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AmbariInstallComponentsRequest.class);
    }

    @Override
    public void accept(Event<AmbariInstallComponentsRequest> event) {
        AmbariInstallComponentsRequest request = event.getData();
        Long stackId = request.getStackId();
        AmbariInstallComponentsResult result;
        try {
            clusterUpscaleService.installComponents(stackId, request.getComponents(), request.getHostName());
            result = new AmbariInstallComponentsResult(request);
        } catch (Exception e) {
            String message = "Failed to install components on new host";
            LOGGER.error(message, e);
            result = new AmbariInstallComponentsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));

    }
}
