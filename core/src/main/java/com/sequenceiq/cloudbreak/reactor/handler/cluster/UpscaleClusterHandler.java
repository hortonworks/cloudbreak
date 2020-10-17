package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscaleClusterHandler implements EventHandler<UpscaleClusterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleClusterHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleClusterRequest.class);
    }

    @Override
    public void accept(Event<UpscaleClusterRequest> event) {
        LOGGER.debug("UpscaleClusterHandler for {}", event.getData().getResourceId());
        Instant start = Instant.now();
        UpscaleClusterRequest request = event.getData();
        UpscaleClusterResult result;
        try {
            clusterUpscaleService.installServicesOnNewHosts(request.getResourceId(), request.getHostGroupName(),
                    request.isRepair(), request.isRestartServices());
            result = new UpscaleClusterResult(request);
        } catch (Exception e) {
            result = new UpscaleClusterResult(e.getMessage(), e, request);
        } finally {
            LOGGER.debug("UpscaleClusterHandler for {} finished in {}ms", event.getData().getResourceId(), Duration.between(start, Instant.now()).toMillis());
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
