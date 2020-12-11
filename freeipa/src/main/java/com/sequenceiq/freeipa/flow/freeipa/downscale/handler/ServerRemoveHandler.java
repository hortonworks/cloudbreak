package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver.RemoveServersRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver.RemoveServersResponse;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ServerRemoveHandler implements EventHandler<RemoveServersRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRemoveHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CleanupService cleanupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveServersRequest.class);
    }

    @Override
    public void accept(Event<RemoveServersRequest> event) {
        RemoveServersRequest request = event.getData();
        try {
            Set<String> hosts = request.getHosts();
            LOGGER.debug("Removing servers [{}]", hosts);
            Pair<Set<String>, Map<String, String>> removeServersResult =
                    cleanupService.removeServers(request.getResourceId(), hosts);
            RemoveServersResponse response = new RemoveServersResponse(request, removeServersResult.getFirst(), removeServersResult.getSecond());
            eventBus.notify(response.getServerCleanupFailed().isEmpty()
                            ? EventSelectorUtil.selector(RemoveServersResponse.class) : EventSelectorUtil.failureSelector(RemoveServersResponse.class),
                    new Event<>(event.getHeaders(), response));
        } catch (Exception e) {
            LOGGER.error("Removing failed for servers: [{}]", request.getHosts(), e);
            Map<String, String> failureResult = request.getHosts().stream().collect(Collectors.toMap(h -> h, h -> e.getMessage()));
            RemoveServersResponse response = new RemoveServersResponse(request, Collections.emptySet(), failureResult);
            eventBus.notify(EventSelectorUtil.failureSelector(RemoveServersResponse.class),
                    new Event<>(event.getHeaders(), response));
        }
    }
}
